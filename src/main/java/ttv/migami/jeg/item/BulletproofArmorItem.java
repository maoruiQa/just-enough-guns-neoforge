package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BulletproofArmorItem extends Item {
    public enum Tier {
        I("i", 1, 1, 55, 80, SoundEvents.ARMOR_EQUIP_LEATHER),
        II("ii", 2, 2, 55, 80, SoundEvents.ARMOR_EQUIP_LEATHER),
        III("iii", 3, 4, 165, 240, SoundEvents.ARMOR_EQUIP_IRON),
        IV("iv", 4, 5, 165, 240, SoundEvents.ARMOR_EQUIP_IRON),
        V("v", 5, 7, 165, 240, SoundEvents.ARMOR_EQUIP_DIAMOND),
        VI("vi", 6, 8, 203, 320, SoundEvents.ARMOR_EQUIP_NETHERITE);

        private final String suffix;
        private final int tierNumber;
        private final int projectileLevel;
        private final int helmetDurability;
        private final int vestDurability;
        private final Holder<SoundEvent> equipSound;

        Tier(String suffix, int tierNumber, int projectileLevel, int helmetDurability, int vestDurability, Holder<SoundEvent> equipSound) {
            this.suffix = suffix;
            this.tierNumber = tierNumber;
            this.projectileLevel = projectileLevel;
            this.helmetDurability = helmetDurability;
            this.vestDurability = vestDurability;
            this.equipSound = equipSound;
        }

        public String suffix() {
            return suffix;
        }

        public int tierNumber() {
            return tierNumber;
        }

        public int projectileLevel() {
            return projectileLevel;
        }

        public int durability(EquipmentSlot slot) {
            return slot == EquipmentSlot.HEAD ? helmetDurability : vestDurability;
        }

        public Holder<SoundEvent> equipSound() {
            return equipSound;
        }
    }

    private final Tier tier;
    private final EquipmentSlot slot;
    @Nullable
    private static Holder<Enchantment> PROJECTILE_PROTECTION;

    public BulletproofArmorItem(Tier tier, EquipmentSlot slot, Properties properties) {
        super(applyProperties(properties, tier, slot));
        this.tier = tier;
        this.slot = slot;
    }

    private static Properties applyProperties(Properties base, Tier tier, EquipmentSlot slot) {
        Equippable equippable = Equippable.builder(slot)
                .setEquipSound(tier.equipSound())
                .setAsset(assetForTier(tier))
                .setEquipOnInteract(true)
                .setDamageOnHurt(true)
                .build();
        return base
                .stacksTo(1)
                .durability(tier.durability(slot))
                .component(DataComponents.EQUIPPABLE, equippable);
    }

    public Tier tier() {
        return tier;
    }

    public EquipmentSlot slot() {
        return slot;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipAdder, flag);
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_tier", tier.tierNumber()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_projectile", tier.projectileLevel()).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }

    @Nullable
    private static Holder<Enchantment> resolveProjectileProtection(RegistryAccess registryAccess) {
        if (PROJECTILE_PROTECTION != null) {
            return PROJECTILE_PROTECTION;
        }

        HolderLookup<Enchantment> lookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        PROJECTILE_PROTECTION = lookup.get(Enchantments.PROJECTILE_PROTECTION).orElse(null);
        return PROJECTILE_PROTECTION;
    }

    private void ensureDefaultEnchant(ItemStack stack, RegistryAccess registryAccess) {
        Holder<Enchantment> protection = resolveProjectileProtection(registryAccess);
        if (protection == null) {
            return;
        }

        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int existing = current.getLevel(protection);
        if (existing == tier.projectileLevel()) {
            return;
        }

        ItemEnchantments.Mutable updated = new ItemEnchantments.Mutable(current);
        updated.removeIf(holder -> holder == protection);
        if (tier.projectileLevel() > 0) {
            updated.set(protection, tier.projectileLevel());
        }
        stack.set(DataComponents.ENCHANTMENTS, updated.toImmutable());
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @NotNull EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (slot == this.slot && entity instanceof LivingEntity living) {
            ItemStack equipped = living.getItemBySlot(slot);
            if (ItemStack.isSameItemSameComponents(equipped, stack)) {
                ensureDefaultEnchant(stack, level.registryAccess());
            }
        }
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Player player) {
        super.onCraftedBy(stack, player);
        ensureDefaultEnchant(stack, player.registryAccess());
    }

    public static boolean isBulletproof(ItemStack stack) {
        return stack.getItem() instanceof BulletproofArmorItem;
    }

    public static boolean isBulletproof(ItemStack stack, Tier tier) {
        if (!(stack.getItem() instanceof BulletproofArmorItem item)) {
            return false;
        }
        return item.tier == tier;
    }

    public int projectileLevel() {
        return tier.projectileLevel();
    }

    private static ResourceKey<EquipmentAsset> assetForTier(Tier tier) {
        // Tier I-II: Leather appearance
        // Tier III+: Enchanted iron appearance (enchanted due to isFoil())
        return switch (tier) {
            case I, II -> EquipmentAssets.LEATHER;
            default -> EquipmentAssets.IRON;
        };
    }
}
