package ttv.migami.jeg.item;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.util.HudMessageHelper;

public final class MagazineItem extends Item {
    private static final ResourceLocation ECHO_SHARD_ID = ResourceLocation.withDefaultNamespace("echo_shard");
    private static final ResourceLocation MAGAZINE_LOAD_1_SOUND_ID = Reference.id("item.magazine.load1");
    private static final ResourceLocation MAGAZINE_LOAD_2_SOUND_ID = Reference.id("item.magazine.load2");
    private static final ResourceLocation MAGAZINE_LOAD_3_SOUND_ID = Reference.id("item.magazine.load3");
    private static final ResourceLocation MAGAZINE_UNLOAD_SOUND_ID = Reference.id("item.magazine.unload");
    private static final Set<ResourceLocation> SUPPORTED_AMMO = Set.of(
            Reference.id("pistol_ammo"),
            Reference.id("rifle_ammo"),
            Reference.id("shotgun_shell"),
            Reference.id("handmade_shell"),
            Reference.id("spectre_round"),
            Reference.id("blaze_round"),
            ECHO_SHARD_ID
    );

    public enum MagazineType {
        PISTOL(12, 5, Set.of(Reference.id("pistol_ammo")), null),
        SMG(32, 5, Set.of(Reference.id("pistol_ammo")), null),
        SMG_EXTENDED(48, 5, Set.of(Reference.id("pistol_ammo")), SMG),
        SMG_DRUM(64, 5, Set.of(Reference.id("pistol_ammo")), SMG),
        RIFLE(30, 5, Set.of(Reference.id("rifle_ammo"), Reference.id("blaze_round"), Reference.id("spectre_round"), ECHO_SHARD_ID), null),
        RIFLE_EXTENDED(45, 5, Set.of(Reference.id("rifle_ammo"), Reference.id("blaze_round"), Reference.id("spectre_round"), ECHO_SHARD_ID), RIFLE),
        RIFLE_DRUM(60, 5, Set.of(Reference.id("rifle_ammo"), Reference.id("blaze_round"), Reference.id("spectre_round"), ECHO_SHARD_ID), RIFLE),
        SHOTGUN(8, 5, Set.of(Reference.id("shotgun_shell"), Reference.id("handmade_shell"), Reference.id("spectre_round"), ECHO_SHARD_ID), null),
        SHOTGUN_EXTENDED(12, 5, Set.of(Reference.id("shotgun_shell"), Reference.id("handmade_shell"), Reference.id("spectre_round"), ECHO_SHARD_ID), SHOTGUN),
        SHOTGUN_DRUM(16, 5, Set.of(Reference.id("shotgun_shell"), Reference.id("handmade_shell"), Reference.id("spectre_round"), ECHO_SHARD_ID), SHOTGUN),
        MACHINE_GUN(150, 2, Set.of(Reference.id("rifle_ammo"), Reference.id("blaze_round"), Reference.id("spectre_round"), ECHO_SHARD_ID), null);

        private final int capacity;
        private final int cooldownTicks;
        private final Set<ResourceLocation> compatibleAmmo;
        private final MagazineType baseType;

        MagazineType(int capacity, int cooldownTicks, Set<ResourceLocation> compatibleAmmo, @Nullable MagazineType baseType) {
            this.capacity = capacity;
            this.cooldownTicks = cooldownTicks;
            this.compatibleAmmo = compatibleAmmo;
            this.baseType = baseType == null ? this : baseType;
        }

        public int capacity() {
            return capacity;
        }

        public int cooldownTicks() {
            return cooldownTicks;
        }

        public boolean supports(ResourceLocation ammoId) {
            return compatibleAmmo.contains(ammoId);
        }

        public MagazineType baseType() {
            return baseType;
        }

        public boolean isVariantOf(MagazineType type) {
            return baseType == type;
        }
    }

    private final MagazineType type;

    public MagazineItem(Properties properties, MagazineType type) {
        super(properties);
        this.type = type;
    }

    public MagazineType type() {
        return type;
    }

    public record UnloadResult(boolean transferredAmmo, boolean showOffhandFullPrompt) {}

    private record UnloadState(@Nullable ResourceLocation ammoId, int ammoCount, int receivableCount, boolean promptAllowed) {
        private boolean hasAmmo() {
            return ammoId != null && ammoCount > 0;
        }

        private boolean canTransfer() {
            return hasAmmo() && receivableCount > 0;
        }
    }

    public boolean tryLoad(Level level, Player player, ItemStack magazineStack, ItemStack ammoStack, boolean notify) {
        if (player.getCooldowns().isOnCooldown(magazineStack.getItem())) {
            return false;
        }

        int ammoCount = getAmmoCount(magazineStack);
        if (ammoCount >= type.capacity()) {
            if (notify) {
                HudMessageHelper.showActionBar(player, Component.translatable("item.jeg.magazine.full"));
            }
            return false;
        }

        Component loadPrompt = getLoadPromptMessage(magazineStack, ammoStack);
        if (loadPrompt != null) {
            if (notify) {
                HudMessageHelper.showActionBar(player, loadPrompt);
            }
            return false;
        }

        ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        if (!player.getAbilities().instabuild) {
            ammoStack.shrink(1);
        }

        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_ITEM.get(), ammoId.toString());
        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), ammoCount + 1);
        player.getCooldowns().addCooldown(magazineStack.getItem(), type.cooldownTicks());
        playLoadSound(level, player);
        return true;
    }

    public boolean canShowUnloadPrompt(ItemStack magazineStack, ItemStack offhandStack) {
        return getUnloadState(magazineStack, offhandStack).promptAllowed();
    }

    public int getOffhandReceivableAmmo(ItemStack magazineStack, ItemStack offhandStack) {
        return getUnloadState(magazineStack, offhandStack).receivableCount();
    }

    public UnloadResult tryUnloadToOffhand(Level level, Player player, ItemStack magazineStack) {
        if (player.getMainHandItem() != magazineStack) {
            return new UnloadResult(false, false);
        }

        UnloadState state = getUnloadState(magazineStack, player.getOffhandItem());
        if (!state.promptAllowed() || !state.hasAmmo()) {
            return new UnloadResult(false, false);
        }
        if (!state.canTransfer()) {
            return new UnloadResult(false, true);
        }

        ResourceLocation ammoId = state.ammoId();
        if (ammoId == null) {
            return new UnloadResult(false, false);
        }

        Item ammoItem = BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
        if (ammoItem == null) {
            return new UnloadResult(false, false);
        }

        int transferCount = Math.min(state.ammoCount(), state.receivableCount());
        ItemStack offhandStack = player.getOffhandItem();
        if (offhandStack.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ammoItem, transferCount));
        } else {
            offhandStack.grow(transferCount);
        }

        int remainingAmmo = state.ammoCount() - transferCount;
        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), remainingAmmo);
        if (remainingAmmo <= 0) {
            magazineStack.remove(ModDataComponents.MAGAZINE_AMMO_ITEM.get());
        }

        playUnloadSound(level, player);
        return new UnloadResult(true, transferCount < state.ammoCount());
    }

    public int getAmmoCount(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), 0), 0, type.capacity());
    }

    public int getCapacity() {
        return type.capacity();
    }

    @Nullable
    public Component getLoadPromptMessage(ItemStack magazineStack, ItemStack ammoStack) {
        if (ammoStack.isEmpty()) {
            return Component.translatable("item.jeg.magazine.no_ammo").withStyle(ChatFormatting.RED);
        }

        ResourceLocation ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        if (!SUPPORTED_AMMO.contains(ammoId)) {
            return Component.translatable("item.jeg.magazine.no_ammo").withStyle(ChatFormatting.RED);
        }
        if (!type.supports(ammoId)) {
            return Component.translatable("item.jeg.magazine.incompatible_ammo").withStyle(ChatFormatting.RED);
        }

        int ammoCount = getAmmoCount(magazineStack);
        ResourceLocation storedAmmoId = ammoCount > 0 ? getAmmoItemId(magazineStack) : null;
        if (storedAmmoId != null && !storedAmmoId.equals(ammoId)) {
            return Component.translatable("item.jeg.magazine.incompatible_ammo").withStyle(ChatFormatting.RED);
        }
        return null;
    }

    @Nullable
    public ResourceLocation getAmmoItemId(ItemStack stack) {
        String stored = stack.get(ModDataComponents.MAGAZINE_AMMO_ITEM.get());
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(stored);
    }

    private UnloadState getUnloadState(ItemStack magazineStack, ItemStack offhandStack) {
        int ammoCount = getAmmoCount(magazineStack);
        ResourceLocation ammoId = ammoCount > 0 ? getAmmoItemId(magazineStack) : null;
        if (ammoCount <= 0 || ammoId == null) {
            return new UnloadState(null, ammoCount, 0, false);
        }

        if (offhandStack.isEmpty()) {
            Item ammoItem = BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
            int receivableCount = ammoItem != null ? ammoItem.getDefaultMaxStackSize() : 0;
            return new UnloadState(ammoId, ammoCount, Math.max(0, receivableCount), true);
        }

        ResourceLocation offhandAmmoId = BuiltInRegistries.ITEM.getKey(offhandStack.getItem());
        if (!ammoId.equals(offhandAmmoId)) {
            return new UnloadState(ammoId, ammoCount, 0, false);
        }

        int receivableCount = Math.max(0, offhandStack.getMaxStackSize() - offhandStack.getCount());
        return new UnloadState(ammoId, ammoCount, receivableCount, true);
    }

    private void playLoadSound(Level level, Player player) {
        ResourceLocation soundId = switch (level.getRandom().nextInt(3)) {
            case 1 -> MAGAZINE_LOAD_2_SOUND_ID;
            case 2 -> MAGAZINE_LOAD_3_SOUND_ID;
            default -> MAGAZINE_LOAD_1_SOUND_ID;
        };
        playSound(level, player, soundId);
    }

    private void playUnloadSound(Level level, Player player) {
        playSound(level, player, MAGAZINE_UNLOAD_SOUND_ID);
    }

    private void playSound(Level level, Player player, ResourceLocation soundId) {
        SoundEvent sound = resolveSound(soundId);
        if (sound == null) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    private SoundEvent resolveSound(ResourceLocation soundId) {
        var holder = ModSounds.ALL.get(soundId);
        return holder != null ? holder.get() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int ammoCount = getAmmoCount(stack);
        tooltip.add(Component.translatable("info.jeg.ammo", ammoCount, type.capacity()));

        ResourceLocation ammoId = ammoCount > 0 ? getAmmoItemId(stack) : null;
        if (ammoId == null) {
            tooltip.add(Component.translatable(
                    "info.jeg.ammo_type",
                    Component.translatable("tooltip.jeg.magazine.empty").copy().withStyle(ChatFormatting.GRAY)
            ));
            return;
        }

        Component ammoName = BuiltInRegistries.ITEM.getOptional(ammoId)
                .map(ItemStack::new)
                .map(ItemStack::getHoverName)
                .orElse(Component.literal(ammoId.toString()));
        tooltip.add(Component.translatable("info.jeg.ammo_type", ammoName));
    }
}
