package ttv.migami.jeg.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachmentRules;
import ttv.migami.jeg.Reference;

public class AttachmentItem extends Item {
    private final AttachmentType type;
    private final AttachmentModifiers modifiers;

    public AttachmentItem(AttachmentType type, AttachmentModifiers modifiers, Properties properties) {
        super(properties);
        this.type = type;
        this.modifiers = modifiers;
    }

    public AttachmentType type() {
        return this.type;
    }

    public AttachmentModifiers modifiers() {
        return this.modifiers;
    }

    public boolean canAttachTo(ItemStack gunStack) {
        return GunAttachmentRules.canAttach(gunStack, this.type);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        tooltipAdder.accept(Component.translatable("info.jeg.attachment_item").withStyle(ChatFormatting.YELLOW));
        if (this.type == AttachmentType.MAGAZINE && Reference.id("drum_mag").equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            tooltipAdder.accept(Component.translatable("info.jeg.attachment_drum_mag_magazine_feed_disabled").withStyle(ChatFormatting.YELLOW));
        }

        List<Component> perks = new ArrayList<>();
        if (this.modifiers.silenced()) {
            addPerk(perks, true, "perk.jeg.silenced.positive");
        }
        if (this.modifiers.fireSoundRadiusMultiplier() < 1.0D) {
            addPerk(perks, true, "perk.jeg.sound_radius.positive");
        } else if (this.modifiers.fireSoundRadiusMultiplier() > 1.0D) {
            addPerk(perks, false, "perk.jeg.sound_radius.negative");
        }
        if (this.modifiers.explosiveAmmo()) {
            addPerk(perks, true, "perk.jeg.explosive_ammo.positive");
            addPerk(perks, false, "perk.jeg.armor_piercing.negative");
        }
        if (this.modifiers.flashlight()) {
            addPerk(perks, true, "perk.jeg.flashlight.positive");
        }
        if (this.modifiers.laserPointer()) {
            addPerk(perks, true, "perk.jeg.laser_pointer.positive");
        }
        if (this.modifiers.annoying()) {
            addPerk(perks, true, "perk.jeg.annoying.positive");
        }
        if (this.modifiers.increasedJamming()) {
            addPerk(perks, false, "perk.jeg.increased_jamming.negative");
        }
        if (this.modifiers.damageMultiplier() > 1.0F) {
            addPerk(perks, true, "perk.jeg.modified_damage.positive");
        } else if (this.modifiers.damageMultiplier() < 1.0F) {
            addPerk(perks, false, "perk.jeg.modified_damage.negative");
        }
        if (this.modifiers.spreadMultiplier() < 1.0F) {
            addPerk(perks, true, "perk.jeg.projectile_spread.positive");
        } else if (this.modifiers.spreadMultiplier() > 1.0F) {
            addPerk(perks, false, "perk.jeg.projectile_spread.negative");
        }
        if (this.modifiers.recoilMultiplier() < 1.0F || this.modifiers.kickMultiplier() < 1.0F) {
            addPerk(perks, true, "perk.jeg.recoil.positive");
        } else if (this.modifiers.recoilMultiplier() > 1.0F || this.modifiers.kickMultiplier() > 1.0F) {
            addPerk(perks, false, "perk.jeg.recoil.negative");
        }
        if (this.modifiers.adsSpeedMultiplier() > 1.0D) {
            addPerk(perks, true, "perk.jeg.ads_speed.positive");
        } else if (this.modifiers.adsSpeedMultiplier() < 1.0D) {
            addPerk(perks, false, "perk.jeg.ads_speed.negative");
        }

        if (!perks.isEmpty()) {
            tooltipAdder.accept(Component.translatable("perk.jeg.title").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
            perks.forEach(tooltipAdder);
        }
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.BINDING_CURSE) || super.supportsEnchantment(stack, enchantment);
    }

    private static void addPerk(List<Component> perks, boolean positive, String key) {
        perks.add(Component.translatable(
                positive ? "perk.jeg.entry.positive" : "perk.jeg.entry.negative",
                Component.translatable(key).withStyle(ChatFormatting.WHITE)
        ).withStyle(positive ? ChatFormatting.DARK_AQUA : ChatFormatting.GOLD));
    }
}
