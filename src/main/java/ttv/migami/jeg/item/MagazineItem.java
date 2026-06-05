package ttv.migami.jeg.item;

import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.util.HudMessageHelper;

public final class MagazineItem extends Item {
    private static final Identifier ECHO_SHARD_ID = Identifier.fromNamespaceAndPath("minecraft", "echo_shard");
    private static final Identifier MAGAZINE_LOAD_1_SOUND_ID = Reference.id("item.magazine.load1");
    private static final Identifier MAGAZINE_LOAD_2_SOUND_ID = Reference.id("item.magazine.load2");
    private static final Identifier MAGAZINE_LOAD_3_SOUND_ID = Reference.id("item.magazine.load3");
    private static final Identifier MAGAZINE_UNLOAD_SOUND_ID = Reference.id("item.magazine.unload");
    private static final Set<Identifier> SUPPORTED_AMMO = Set.of(
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
        private final Set<Identifier> compatibleAmmo;
        private final MagazineType baseType;

        MagazineType(int capacity, int cooldownTicks, Set<Identifier> compatibleAmmo, @Nullable MagazineType baseType) {
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

        public boolean supports(Identifier ammoId) {
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

    private record UnloadState(@Nullable Identifier ammoId, int ammoCount, int receivableCount, boolean promptAllowed) {
        private boolean hasAmmo() {
            return ammoId != null && ammoCount > 0;
        }

        private boolean canTransfer() {
            return hasAmmo() && receivableCount > 0;
        }
    }

    public boolean tryLoad(Level level, Player player, ItemStack magazineStack, ItemStack ammoStack, boolean notify) {
        if (player.getCooldowns().isOnCooldown(magazineStack)) {
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

        Identifier ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        if (!player.getAbilities().instabuild) {
            ammoStack.shrink(1);
        }

        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_ITEM.get(), ammoId.toString());
        magazineStack.set(ModDataComponents.MAGAZINE_AMMO_COUNT.get(), ammoCount + 1);
        player.getCooldowns().addCooldown(magazineStack, type.cooldownTicks());
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

        Identifier ammoId = state.ammoId();
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
        boolean showOffhandFullPrompt = transferCount < state.ammoCount();
        return new UnloadResult(true, showOffhandFullPrompt);
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

        Identifier ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
        if (!SUPPORTED_AMMO.contains(ammoId)) {
            return Component.translatable("item.jeg.magazine.no_ammo").withStyle(ChatFormatting.RED);
        }
        if (!type.supports(ammoId)) {
            return Component.translatable("item.jeg.magazine.incompatible_ammo").withStyle(ChatFormatting.RED);
        }

        int ammoCount = getAmmoCount(magazineStack);
        Identifier storedAmmoId = ammoCount > 0 ? getAmmoItemId(magazineStack) : null;
        if (storedAmmoId != null && !storedAmmoId.equals(ammoId)) {
            return Component.translatable("item.jeg.magazine.incompatible_ammo").withStyle(ChatFormatting.RED);
        }
        return null;
    }

    @Nullable
    public Identifier getAmmoItemId(ItemStack stack) {
        String stored = stack.get(ModDataComponents.MAGAZINE_AMMO_ITEM.get());
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return Identifier.tryParse(stored);
    }

    private UnloadState getUnloadState(ItemStack magazineStack, ItemStack offhandStack) {
        int ammoCount = getAmmoCount(magazineStack);
        Identifier ammoId = ammoCount > 0 ? getAmmoItemId(magazineStack) : null;
        if (ammoCount <= 0 || ammoId == null) {
            return new UnloadState(null, ammoCount, 0, false);
        }

        if (offhandStack.isEmpty()) {
            Item ammoItem = BuiltInRegistries.ITEM.getOptional(ammoId).orElse(null);
            int receivableCount = ammoItem != null ? ammoItem.getDefaultMaxStackSize() : 0;
            return new UnloadState(ammoId, ammoCount, Math.max(0, receivableCount), true);
        }

        Identifier offhandAmmoId = BuiltInRegistries.ITEM.getKey(offhandStack.getItem());
        if (!ammoId.equals(offhandAmmoId)) {
            return new UnloadState(ammoId, ammoCount, 0, false);
        }

        int receivableCount = Math.max(0, offhandStack.getMaxStackSize() - offhandStack.getCount());
        return new UnloadState(ammoId, ammoCount, receivableCount, true);
    }

    private void playLoadSound(Level level, Player player) {
        Identifier soundId = switch (level.getRandom().nextInt(3)) {
            case 1 -> MAGAZINE_LOAD_2_SOUND_ID;
            case 2 -> MAGAZINE_LOAD_3_SOUND_ID;
            default -> MAGAZINE_LOAD_1_SOUND_ID;
        };
        playSound(level, player, soundId);
    }

    private void playUnloadSound(Level level, Player player) {
        playSound(level, player, MAGAZINE_UNLOAD_SOUND_ID);
    }

    private void playSound(Level level, Player player, Identifier soundId) {
        SoundEvent sound = resolveSound(soundId);
        if (sound == null) {
            return;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Nullable
    private SoundEvent resolveSound(Identifier soundId) {
        var holder = ModSounds.ALL.get(soundId);
        return holder != null ? holder.get() : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        int ammoCount = getAmmoCount(stack);
        tooltipAdder.accept(Component.translatable("info.jeg.ammo", ammoCount, type.capacity()));

        Identifier ammoId = ammoCount > 0 ? getAmmoItemId(stack) : null;
        if (ammoId == null) {
            tooltipAdder.accept(Component.translatable("info.jeg.ammo_type",
                    Component.translatable("tooltip.jeg.magazine.empty").copy().withStyle(ChatFormatting.GRAY)));
            return;
        }

        Component ammoName = BuiltInRegistries.ITEM.getOptional(ammoId)
                .map(ItemStack::new)
                .map(ItemStack::getHoverName)
                .orElse(Component.literal(ammoId.toString()));
        tooltipAdder.accept(Component.translatable("info.jeg.ammo_type", ammoName));
    }
}
