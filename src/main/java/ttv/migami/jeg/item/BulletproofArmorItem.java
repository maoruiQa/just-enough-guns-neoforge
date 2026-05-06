package ttv.migami.jeg.item;

import java.util.function.Consumer;
import java.util.EnumMap;
import java.util.List;
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
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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
    private static final Holder<ArmorMaterial> TIER_I_VEST_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_vest_i");
    private static final Holder<ArmorMaterial> TIER_II_VEST_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_vest_ii");
    private static final Holder<ArmorMaterial> TIER_III_VEST_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_vest_iii");
    private static final Holder<ArmorMaterial> TIER_IV_VEST_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_vest_iv");
    private static final Holder<ArmorMaterial> TIER_V_VEST_MATERIAL = vestMaterial(ArmorMaterials.DIAMOND, "bulletproof_vest_v");
    private static final Holder<ArmorMaterial> TIER_VI_VEST_MATERIAL = vestMaterial(ArmorMaterials.NETHERITE, "bulletproof_vest_vi");

    public enum Tier {
        I("i", 1, 1, 55, 80, 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        II("ii", 2, 2, 55, 80, 1, 2, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        III("iii", 3, 4, 165, 240, 1, 3, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        IV("iv", 4, 5, 165, 240, 1, 4, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        V("v", 5, 7, 165, 240, 2, 4, SoundEvents.ARMOR_EQUIP_DIAMOND, ArmorMaterials.DIAMOND),
        VI("vi", 6, 8, 203, 320, 2, 5, SoundEvents.ARMOR_EQUIP_NETHERITE, ArmorMaterials.NETHERITE);

        private final String suffix;
        private final int tierNumber;
        private final int projectileLevel;
        private final int helmetDurability;
        private final int vestDurability;
        private final int helmetArmor;
        private final int vestArmor;
        private final Holder<SoundEvent> equipSound;
        private final Holder<ArmorMaterial> material;

        Tier(String suffix, int tierNumber, int projectileLevel, int helmetDurability, int vestDurability, int helmetArmor, int vestArmor,
              Holder<SoundEvent> equipSound, Holder<ArmorMaterial> material) {
            this.suffix = suffix;
            this.tierNumber = tierNumber;
            this.projectileLevel = projectileLevel;
            this.helmetDurability = helmetDurability;
            this.vestDurability = vestDurability;
            this.helmetArmor = helmetArmor;
            this.vestArmor = vestArmor;
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

        public int armor(Type armorType) {
            return armorType == Type.HELMET ? helmetArmor : vestArmor;
        }

        public Holder<SoundEvent> equipSound() {
            return equipSound;
        }

        public Holder<ArmorMaterial> material() {
            return material;
        }
    }

    private final Tier tier;
    private final ItemAttributeModifiers defaultAttributeModifiers;

    public BulletproofArmorItem(Tier tier, EquipmentSlot slot, Properties properties) {
        super(materialFor(tier, slot), convertSlotToType(slot), applyProperties(properties, tier, slot));
        this.tier = tier;
        this.defaultAttributeModifiers = buildArmorModifiers(tier, this.getType());
    }

    private static Holder<ArmorMaterial> materialFor(Tier tier, EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST) {
            return switch (tier) {
                case I -> TIER_I_VEST_MATERIAL;
                case II -> TIER_II_VEST_MATERIAL;
                case III -> TIER_III_VEST_MATERIAL;
                case IV -> TIER_IV_VEST_MATERIAL;
                case V -> TIER_V_VEST_MATERIAL;
                case VI -> TIER_VI_VEST_MATERIAL;
                default -> tier.material();
            };
        }
        return tier.material();
    }

    private static Holder<ArmorMaterial> vestMaterial(Holder<ArmorMaterial> baseMaterial, String textureName) {
        ArmorMaterial base = baseMaterial.value();
        return Holder.direct(new ArmorMaterial(
                new EnumMap<>(base.defense()),
                base.enchantmentValue(),
                base.equipSound(),
                base.repairIngredient(),
                List.of(new ArmorMaterial.Layer(ttv.migami.jeg.Reference.id(textureName))),
                base.toughness(),
                base.knockbackResistance()
        ));
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

    private static ItemAttributeModifiers buildArmorModifiers(Tier tier, Type armorType) {
        int armor = tier.armor(armorType);
        if (armor <= 0) {
            return ItemAttributeModifiers.EMPTY;
        }

        EquipmentSlot slot = armorType.getSlot();
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ARMOR,
                        new AttributeModifier(
                                ttv.migami.jeg.Reference.id("bulletproof_armor_" + tier.suffix() + "_" + slotKey(slot)),
                                armor,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.bySlot(slot)
                )
                .build();
    }

    private static String slotKey(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ? "helmet" : "vest";
    }

    public Tier tier() {
        return tier;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return this.defaultAttributeModifiers;
    }

    @Override
    public int getDefense() {
        return this.tier.armor(this.getType());
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
        HolderLookup<Enchantment> lookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        return lookup.get(Enchantments.PROJECTILE_PROTECTION).orElse(null);
    }

    private void ensureDefaultEnchant(ItemStack stack, RegistryAccess registryAccess) {
        Holder<Enchantment> protection = resolveProjectileProtection(registryAccess);
        if (protection == null) {
            return;
        }

        ItemEnchantments current = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable updated = new ItemEnchantments.Mutable(current);
        updated.removeIf(holder -> holder.is(Enchantments.PROJECTILE_PROTECTION));
        if (tier.projectileLevel() > 0) {
            updated.set(protection, tier.projectileLevel());
        }
        ItemEnchantments normalized = updated.toImmutable();
        if (!normalized.equals(current)) {
            stack.set(DataComponents.ENCHANTMENTS, normalized);
        }
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
