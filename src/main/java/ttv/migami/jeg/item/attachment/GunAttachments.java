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
        return true;
    }

    public static void clear(ItemStack gunStack, AttachmentType type) {
        gunStack.remove(component(type));
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
