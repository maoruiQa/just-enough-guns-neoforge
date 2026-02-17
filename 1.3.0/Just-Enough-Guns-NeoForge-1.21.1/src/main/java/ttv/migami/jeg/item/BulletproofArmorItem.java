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
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bulletproof armor for 1.21.1 - Adapted from 1.21.8 version.
 * Uses ArmorItem base class since Equipment API is not available in 1.21.1.
 */
public class BulletproofArmorItem extends ArmorItem {
    public enum Tier {
        I("i", 1, 1, 55, 80, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        II("ii", 2, 2, 55, 80, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        III("iii", 3, 4, 165, 240, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        IV("iv", 4, 5, 165, 240, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        V("v", 5, 7, 165, 240, SoundEvents.ARMOR_EQUIP_DIAMOND, ArmorMaterials.DIAMOND),
        VI("vi", 6, 8, 203, 320, SoundEvents.ARMOR_EQUIP_NETHERITE, ArmorMaterials.NETHERITE);

        private final String suffix;
        private final int tierNumber;
        private final int projectileLevel;
        private final int helmetDurability;
        private final int vestDurability;
        private final Holder<SoundEvent> equipSound;
        private final Holder<ArmorMaterial> material;

        Tier(String suffix, int tierNumber, int projectileLevel, int helmetDurability, int vestDurability,
             Holder<SoundEvent> equipSound, Holder<ArmorMaterial> material) {
            this.suffix = suffix;
            this.tierNumber = tierNumber;
            this.projectileLevel = projectileLevel;
            this.helmetDurability = helmetDurability;
            this.vestDurability = vestDurability;
            this.equipSound = equipSound;
            this.material = material;
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

        public int durability(Type armorType) {
            return armorType == Type.HELMET ? helmetDurability : vestDurability;
        }

        public Holder<SoundEvent> equipSound() {
            return equipSound;
        }

        public Holder<ArmorMaterial> material() {
            return material;
        }
    }

    private final Tier tier;
    @Nullable
    private static Holder<Enchantment> PROJECTILE_PROTECTION;

    public BulletproofArmorItem(Tier tier, EquipmentSlot slot, Properties properties) {
        super(tier.material(), convertSlotToType(slot), applyProperties(properties, tier, slot));
        this.tier = tier;
    }

    private static Type convertSlotToType(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Type.HELMET;
            case CHEST -> Type.CHESTPLATE;
            case LEGS -> Type.LEGGINGS;
            case FEET -> Type.BOOTS;
            default -> Type.CHESTPLATE;
        };
    }

    private static Properties applyProperties(Properties base, Tier tier, EquipmentSlot slot) {
        return base
                .stacksTo(1)
                .durability(tier.durability(convertSlotToType(slot)));
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull java.util.List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_tier", tier.tierNumber()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_projectile", tier.projectileLevel()).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
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
    public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            // Check if this item is equipped in the correct slot
            EquipmentSlot correctSlot = this.getType().getSlot();
            ItemStack equipped = living.getItemBySlot(correctSlot);
            if (ItemStack.isSameItemSameComponents(equipped, stack) && level instanceof ServerLevel serverLevel) {
                ensureDefaultEnchant(stack, serverLevel.registryAccess());
            }
        }
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide()) {
            ensureDefaultEnchant(stack, player.registryAccess());
        }
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
}
