package ttv.migami.jeg.item.attachment;

import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.AttachmentItem;

public final class GunAttachments {
    private GunAttachments() {
    }

    public static boolean has(ItemStack gunStack, AttachmentType type) {
        return id(gunStack, type).isPresent();
    }

    public static Optional<ResourceLocation> id(ItemStack gunStack, AttachmentType type) {
        String raw = gunStack.get(component(type));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(raw));
    }

    public static Optional<AttachmentItem> item(ItemStack gunStack, AttachmentType type) {
        return id(gunStack, type)
                .flatMap(BuiltInRegistries.ITEM::getOptional)
                .filter(AttachmentItem.class::isInstance)
                .map(AttachmentItem.class::cast);
    }

    public static AttachmentModifiers modifiers(ItemStack gunStack, AttachmentType type) {
        return item(gunStack, type).map(AttachmentItem::modifiers).orElse(AttachmentModifiers.NONE);
    }

    public static AttachmentModifiers modifiers(ItemStack gunStack) {
        float aimFovModifier = 0.0F;
        float damageMultiplier = 1.0F;
        float spreadMultiplier = 1.0F;
        float recoilMultiplier = 1.0F;
        float kickMultiplier = 1.0F;
        double adsSpeedMultiplier = 1.0D;
        double magazineCapacityMultiplier = 1.0D;
        boolean silenced = false;
        boolean explosiveAmmo = false;
        boolean flashlight = false;
        boolean laserPointer = false;
        boolean annoying = false;

        for (AttachmentType type : AttachmentType.values()) {
            AttachmentModifiers modifier = modifiers(gunStack, type);
            if (modifier.aimFovModifier() > 0.0F) {
                aimFovModifier = modifier.aimFovModifier();
            }
            damageMultiplier *= modifier.damageMultiplier();
            spreadMultiplier *= modifier.spreadMultiplier();
            recoilMultiplier *= modifier.recoilMultiplier();
            kickMultiplier *= modifier.kickMultiplier();
            adsSpeedMultiplier *= modifier.adsSpeedMultiplier();
            magazineCapacityMultiplier *= modifier.magazineCapacityMultiplier();
            silenced |= modifier.silenced();
            explosiveAmmo |= modifier.explosiveAmmo();
            flashlight |= modifier.flashlight();
            laserPointer |= modifier.laserPointer();
            annoying |= modifier.annoying();
        }

        return new AttachmentModifiers(
                aimFovModifier,
                damageMultiplier,
                spreadMultiplier,
                recoilMultiplier,
                kickMultiplier,
                adsSpeedMultiplier,
                magazineCapacityMultiplier,
                silenced,
                explosiveAmmo,
                flashlight,
                laserPointer,
                annoying
        );
    }

    public static boolean set(ItemStack gunStack, AttachmentType type, ItemStack attachmentStack) {
        if (attachmentStack.isEmpty()) {
            clear(gunStack, type);
            return true;
        }
        Item item = attachmentStack.getItem();
        if (!(item instanceof AttachmentItem attachment) || attachment.type() != type || !attachment.canAttachTo(gunStack)) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        gunStack.set(component(type), id.toString());
        if (type == AttachmentType.SPECIAL && !attachment.modifiers().flashlight()) {
            gunStack.remove(ModDataComponents.GUN_FLASHLIGHT_POWERED.get());
        }
        return true;
    }

    public static void clear(ItemStack gunStack, AttachmentType type) {
        gunStack.remove(component(type));
        if (type == AttachmentType.SPECIAL) {
            gunStack.remove(ModDataComponents.GUN_FLASHLIGHT_POWERED.get());
        }
    }

    public static boolean hasFlashlight(ItemStack gunStack) {
        return item(gunStack, AttachmentType.SPECIAL)
                .map(AttachmentItem::modifiers)
                .map(AttachmentModifiers::flashlight)
                .orElse(false);
    }

    public static boolean isFlashlightPowered(ItemStack gunStack) {
        return hasFlashlight(gunStack) && Boolean.TRUE.equals(gunStack.get(ModDataComponents.GUN_FLASHLIGHT_POWERED.get()));
    }

    public static boolean toggleFlashlight(ItemStack gunStack) {
        if (!hasFlashlight(gunStack)) {
            return false;
        }
        boolean powered = !isFlashlightPowered(gunStack);
        gunStack.set(ModDataComponents.GUN_FLASHLIGHT_POWERED.get(), powered);
        return true;
    }

    private static DataComponentType<String> component(AttachmentType type) {
        return switch (type) {
            case SCOPE -> ModDataComponents.GUN_SCOPE_ATTACHMENT.get();
            case BARREL -> ModDataComponents.GUN_BARREL_ATTACHMENT.get();
            case STOCK -> ModDataComponents.GUN_STOCK_ATTACHMENT.get();
            case UNDER_BARREL -> ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT.get();
            case MAGAZINE -> ModDataComponents.GUN_MAGAZINE_ATTACHMENT.get();
            case SPECIAL -> ModDataComponents.GUN_SPECIAL_ATTACHMENT.get();
        };
    }
}
