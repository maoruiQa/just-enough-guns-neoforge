package ttv.migami.jeg.client.render.gun;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;

final class GunPaintJobTextures {
    private static final String MODEL_ROOT = "geo/item/gun/";
    private static final String MODEL_PAINT_JOB_ROOT = "geo/item/gun/paintjob/";
    private static final String TEXTURE_ITEM_ROOT = "textures/item/";
    private static final String TEXTURE_ANIM_ROOT = "textures/animated/gun/";
    private static final String TEXTURE_PAINT_JOB_ROOT = "textures/animated/gun/paintjob/";
    private static final String FALLBACK = "abstract_gun";

    private GunPaintJobTextures() {
    }

    static Identifier texture(AnimatedGunItem gun, ItemStack stack) {
        String gunPath = path(gun);
        if (stack != null && !stack.isEmpty()) {
            Identifier paintJobTexture = paintJobTexture(stack, gunPath);
            if (paintJobTexture != null) {
                return paintJobTexture;
            }
        }
        return baseTexture(gunPath);
    }

    static Identifier model(AnimatedGunItem gun, ItemStack stack) {
        String gunPath = path(gun);
        if (stack != null && !stack.isEmpty()) {
            Identifier paintJobModel = paintJobModel(stack, gunPath);
            if (paintJobModel != null) {
                return paintJobModel;
            }
        }
        return baseModel(gunPath);
    }

    static Identifier baseModel(AnimatedGunItem gun) {
        return baseModel(path(gun));
    }

    static Identifier baseTexture(AnimatedGunItem gun) {
        return baseTexture(path(gun));
    }

    private static Identifier paintJobModel(ItemStack stack, String gunPath) {
        String paintJob = paintJob(stack);
        if (paintJob == null) {
            return null;
        }

        Identifier model = Reference.id(MODEL_PAINT_JOB_ROOT + paintJob + "/" + gunPath + ".geo.json");
        return exists(model) ? model : null;
    }

    private static Identifier paintJobTexture(ItemStack stack, String gunPath) {
        String paintJob = paintJob(stack);
        if (paintJob == null) {
            return null;
        }

        Identifier texture = Reference.id(TEXTURE_PAINT_JOB_ROOT + paintJob + "/" + gunPath + ".png");
        return exists(texture) ? texture : null;
    }

    private static String paintJob(ItemStack stack) {
        Item item = GunAttachments.cosmeticItem(stack, AttachmentType.PAINT_JOB).orElse(null);
        if (!(item instanceof PaintJobCanItem paintJobCanItem)) {
            return null;
        }

        String paintJob = paintJobCanItem.paintJob();
        if (paintJob == null || paintJob.isBlank()) {
            return null;
        }
        return paintJob;
    }

    private static Identifier baseModel(String gunPath) {
        return Reference.id(MODEL_ROOT + gunPath + ".geo.json");
    }

    private static Identifier baseTexture(String gunPath) {
        Identifier primary = Reference.id(TEXTURE_ANIM_ROOT + gunPath + ".png");
        Identifier fallback = Reference.id(TEXTURE_ITEM_ROOT + gunPath + ".png");
        return exists(primary) ? primary : fallback;
    }

    private static String path(AnimatedGunItem gun) {
        String path = gun.getStats().id().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

    private static boolean exists(Identifier id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
