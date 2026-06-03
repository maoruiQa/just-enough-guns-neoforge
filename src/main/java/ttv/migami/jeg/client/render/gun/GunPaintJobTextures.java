package ttv.migami.jeg.client.render.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;

final class GunPaintJobTextures {
    private static final String TEXTURE_ITEM_ROOT = "textures/item/";
    private static final String TEXTURE_ANIM_ROOT = "textures/animated/gun/";
    private static final String TEXTURE_PAINT_JOB_ROOT = "textures/animated/gun/paintjob/";
    private static final String FALLBACK = "abstract_gun";

    private GunPaintJobTextures() {
    }

    static ResourceLocation texture(AnimatedGunItem gun, ItemStack stack) {
        String gunPath = path(gun);
        if (stack != null && !stack.isEmpty()) {
            ResourceLocation paintJobTexture = paintJobTexture(stack, gunPath);
            if (paintJobTexture != null) {
                return paintJobTexture;
            }
        }
        return baseTexture(gunPath);
    }

    static ResourceLocation baseTexture(AnimatedGunItem gun) {
        return baseTexture(path(gun));
    }

    private static ResourceLocation paintJobTexture(ItemStack stack, String gunPath) {
        Item item = GunAttachments.cosmeticItem(stack, AttachmentType.PAINT_JOB).orElse(null);
        if (!(item instanceof PaintJobCanItem paintJobCanItem)) {
            return null;
        }

        String paintJob = paintJobCanItem.paintJob();
        if (paintJob == null || paintJob.isBlank()) {
            return null;
        }

        ResourceLocation texture = Reference.id(TEXTURE_PAINT_JOB_ROOT + paintJob + "/" + gunPath + ".png");
        return exists(texture) ? texture : null;
    }

    private static ResourceLocation baseTexture(String gunPath) {
        ResourceLocation primary = Reference.id(TEXTURE_ANIM_ROOT + gunPath + ".png");
        ResourceLocation fallback = Reference.id(TEXTURE_ITEM_ROOT + gunPath + ".png");
        return exists(primary) ? primary : fallback;
    }

    private static String path(AnimatedGunItem gun) {
        String path = gun.getStats().id().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

    private static boolean exists(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
