package ttv.migami.jeg.item;

import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.gun.BallisticProtection;
import ttv.migami.jeg.init.ModItems;

public class BulletproofArmorItem extends Item {
    public enum Tier {
        I("i", 1, 2.10F, 0.30F, 0.85F, 1.20F, 55, 80, 0, 1, SoundEvents.ARMOR_EQUIP_LEATHER),
        II("ii", 2, 3.10F, 0.25F, 0.80F, 1.10F, 55, 80, 1, 2, SoundEvents.ARMOR_EQUIP_LEATHER),
        III("iii", 3, 3.60F, 0.22F, 0.76F, 1.00F, 165, 240, 1, 3, SoundEvents.ARMOR_EQUIP_IRON),
        IV("iv", 4, 4.10F, 0.18F, 0.70F, 0.90F, 165, 240, 1, 4, SoundEvents.ARMOR_EQUIP_IRON),
        V("v", 5, 5.20F, 0.15F, 0.64F, 0.80F, 165, 240, 2, 4, SoundEvents.ARMOR_EQUIP_DIAMOND),
        VI("vi", 6, 6.20F, 0.12F, 0.58F, 0.70F, 203, 320, 2, 5, SoundEvents.ARMOR_EQUIP_NETHERITE);

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

        Tier(String suffix, int tierNumber, float ballisticRating, float undermatchMultiplier, float overmatchMultiplier, float durabilityScale, int helmetDurability, int vestDurability, int helmetArmor, int vestArmor, Holder<SoundEvent> equipSound) {
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

        public int durability(EquipmentSlot slot) {
            return slot == EquipmentSlot.HEAD ? helmetDurability : vestDurability;
        }

        public int armor(EquipmentSlot slot) {
            return slot == EquipmentSlot.HEAD ? helmetArmor : vestArmor;
        }

        public Holder<SoundEvent> equipSound() {
            return equipSound;
        }
    }

    private final Tier tier;
    private final EquipmentSlot slot;

    public BulletproofArmorItem(Tier tier, EquipmentSlot slot, Properties properties) {
        super(applyProperties(properties, tier, slot));
        this.tier = tier;
        this.slot = slot;
    }

    private static Properties applyProperties(Properties base, Tier tier, EquipmentSlot slot) {
        Equippable equippable = Equippable.builder(slot)
                .setEquipSound(tier.equipSound())
                .setAsset(assetForTier(tier, slot))
                .setEquipOnInteract(true)
                .setDamageOnHurt(true)
                .build();
        ItemAttributeModifiers attributes = buildArmorModifiers(tier, slot);
        return base
                .stacksTo(1)
                .durability(tier.durability(slot))
                .repairable(ModItems.REPAIR_KIT.get())
                .component(DataComponents.EQUIPPABLE, equippable)
                .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes);
    }

    private static ItemAttributeModifiers buildArmorModifiers(Tier tier, EquipmentSlot slot) {
        int armor = tier.armor(slot);
        if (armor <= 0) {
            return ItemAttributeModifiers.EMPTY;
        }

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

    public EquipmentSlot slot() {
        return slot;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return false;
    }

    public boolean isValidRepairItem(@NotNull ItemStack stack, @NotNull ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.REPAIR_KIT.get());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull TooltipDisplay display, @NotNull Consumer<Component> tooltipAdder, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltipAdder, flag);
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_tier", tier.tierNumber()).withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_ballistic_rating", String.format(java.util.Locale.US, "%.1f", BallisticProtection.effectiveArmorRating(tier, this.slot))).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_undermatch_multiplier", String.format(java.util.Locale.US, "%.2f", tier.undermatchMultiplier())).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        tooltipAdder.accept(Component.translatable("tooltip.jeg.bulletproof_overmatch_multiplier", String.format(java.util.Locale.US, "%.2f", tier.overmatchMultiplier())).withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull ServerLevel level, @NotNull Entity entity, @NotNull EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Player player) {
        super.onCraftedBy(stack, player);
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

    private static ResourceKey<EquipmentAsset> assetForTier(Tier tier, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) {
            return ResourceKey.create(EquipmentAssets.ROOT_ID, ttv.migami.jeg.Reference.id("bulletproof_helmet_" + tier.suffix()));
        }
        if (slot == EquipmentSlot.CHEST) {
            return ResourceKey.create(EquipmentAssets.ROOT_ID, ttv.migami.jeg.Reference.id("bulletproof_vest_" + tier.suffix()));
        }

        // Tier I-II: Leather appearance
        // Tier III+: Enchanted iron appearance (enchanted due to isFoil())
        return switch (tier) {
            case I, II -> EquipmentAssets.LEATHER;
            case III, IV -> EquipmentAssets.IRON;
            case V -> EquipmentAssets.DIAMOND;
            case VI -> EquipmentAssets.NETHERITE;
        };
    }
}
