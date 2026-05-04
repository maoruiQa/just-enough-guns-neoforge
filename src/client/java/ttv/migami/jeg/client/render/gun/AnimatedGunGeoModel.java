package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunGeoModel extends GeoModel<AnimatedGunItem> {
    private static final String MODEL_ROOT = "geo/item/gun/";
    private static final String ANIMATION_ROOT = "animations/item/";
    private static final String TEXTURE_ITEM_ROOT = "textures/item/";
    private static final String TEXTURE_ANIM_ROOT = "textures/animated/gun/";
    private static final String FALLBACK = "abstract_gun";

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem animatable) {
        return Reference.id(MODEL_ROOT + path(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem animatable) {
        String p = path(animatable);
        ResourceLocation primary = Reference.id(TEXTURE_ANIM_ROOT + p + ".png");
        ResourceLocation fallback = Reference.id(TEXTURE_ITEM_ROOT + p + ".png");
        return exists(primary) ? primary : fallback;
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem animatable) {
        return Reference.id(ANIMATION_ROOT + path(animatable) + ".animation.json");
    }

    private static String path(AnimatedGunItem item) {
        String path = item.getStats().id().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

    private static boolean exists(ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
