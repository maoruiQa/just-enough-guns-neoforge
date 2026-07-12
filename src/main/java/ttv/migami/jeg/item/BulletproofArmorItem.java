package ttv.migami.jeg.item;

import java.util.function.Consumer;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.init.ModItems;

/**
 * Bulletproof armor for 1.21.1 - Adapted from 1.21.8 version.
 * Uses ArmorItem base class since Equipment API is not available in 1.21.1.
 */
public class BulletproofArmorItem extends ArmorItem {
    private static final Holder<ArmorMaterial> TIER_I_HELMET_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_helmet_i");
    private static final Holder<ArmorMaterial> TIER_II_HELMET_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_helmet_ii");
    private static final Holder<ArmorMaterial> TIER_III_HELMET_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_helmet_iii");
    private static final Holder<ArmorMaterial> TIER_IV_HELMET_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_helmet_iv");
    private static final Holder<ArmorMaterial> TIER_V_HELMET_MATERIAL = vestMaterial(ArmorMaterials.DIAMOND, "bulletproof_helmet_v");
    private static final Holder<ArmorMaterial> TIER_VI_HELMET_MATERIAL = vestMaterial(ArmorMaterials.NETHERITE, "bulletproof_helmet_vi");

    private static final Holder<ArmorMaterial> TIER_I_VEST_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_vest_i");
    private static final Holder<ArmorMaterial> TIER_II_VEST_MATERIAL = vestMaterial(ArmorMaterials.LEATHER, "bulletproof_vest_ii");
    private static final Holder<ArmorMaterial> TIER_III_VEST_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_vest_iii");
    private static final Holder<ArmorMaterial> TIER_IV_VEST_MATERIAL = vestMaterial(ArmorMaterials.IRON, "bulletproof_vest_iv");
    private static final Holder<ArmorMaterial> TIER_V_VEST_MATERIAL = vestMaterial(ArmorMaterials.DIAMOND, "bulletproof_vest_v");
    private static final Holder<ArmorMaterial> TIER_VI_VEST_MATERIAL = vestMaterial(ArmorMaterials.NETHERITE, "bulletproof_vest_vi");

    public enum Tier {
        I("i", 1, 2.10F, 0.30F, 0.85F, 1.20F, 55, 80, 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        II("ii", 2, 3.10F, 0.25F, 0.80F, 1.10F, 55, 80, 1, 2, SoundEvents.ARMOR_EQUIP_LEATHER, ArmorMaterials.LEATHER),
        III("iii", 3, 3.60F, 0.22F, 0.76F, 1.00F, 165, 240, 1, 3, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        IV("iv", 4, 4.10F, 0.18F, 0.70F, 0.90F, 165, 240, 1, 4, SoundEvents.ARMOR_EQUIP_IRON, ArmorMaterials.IRON),
        V("v", 5, 5.20F, 0.15F, 0.64F, 0.80F, 165, 240, 2, 4, SoundEvents.ARMOR_EQUIP_DIAMOND, ArmorMaterials.DIAMOND),
        VI("vi", 6, 6.20F, 0.12F, 0.58F, 0.70F, 203, 320, 2, 5, SoundEvents.ARMOR_EQUIP_NETHERITE, ArmorMaterials.NETHERITE);

        private final String suffix;
        private final int tierNumber;
        private final float ballisticRating;
        private final float undermatchMultiplier;
        private final float overmatchMultiplier;
        private final float durabilityScale;
        private final int helmetDurability;
        private final int vestDurability;
        private final int helmetArmor;
        private final int vestArmor;
        private final Holder<SoundEvent> equipSound;
        private final Holder<ArmorMaterial> material;

        Tier(String suffix, int tierNumber, float ballisticRating, float undermatchMultiplier, float overmatchMultiplier, float durabilityScale, int helmetDurability, int vestDurability, int helmetArmor, int vestArmor,
              Holder<SoundEvent> equipSound, Holder<ArmorMaterial> material) {
            this.suffix = suffix;
            this.tierNumber = tierNumber;
            this.ballisticRating = ballisticRating;
            this.undermatchMultiplier = undermatchMultiplier;
            this.overmatchMultiplier = overmatchMultiplier;
            this.durabilityScale = durabilityScale;
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

        public float ballisticRating() {
            return ballisticRating;
        }

        public float undermatchMultiplier() {
            return undermatchMultiplier;
        }

        public float overmatchMultiplier() {
            return overmatchMultiplier;
        }

        public float durabilityScale() {
            return durabilityScale;
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
        if (slot == EquipmentSlot.HEAD) {
            return switch (tier) {
                case I -> TIER_I_HELMET_MATERIAL;
                case II -> TIER_II_HELMET_MATERIAL;
                case III -> TIER_III_HELMET_MATERIAL;
                case IV -> TIER_IV_HELMET_MATERIAL;
                case V -> TIER_V_HELMET_MATERIAL;
                case VI -> TIER_VI_HELMET_MATERIAL;
                default -> tier.material();
            };
        }
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
        return false;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.REPAIR_KIT.get());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull java.util.List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_tier", tier.tierNumber()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_ballistic_rating", String.format(java.util.Locale.US, "%.1f", BallisticProtection.effectiveArmorRating(tier, this.getType().getSlot()))).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_undermatch_multiplier", String.format(java.util.Locale.US, "%.2f", tier.undermatchMultiplier())).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        tooltipComponents.add(Component.translatable("tooltip.jeg.bulletproof_overmatch_multiplier", String.format(java.util.Locale.US, "%.2f", tier.overmatchMultiplier())).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull net.minecraft.world.level.Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
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

}
