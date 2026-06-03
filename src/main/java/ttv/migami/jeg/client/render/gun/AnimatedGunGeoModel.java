package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunGeoModel extends GeoModel<AnimatedGunItem> {
    private static final String MODEL_ROOT = "geo/item/gun/";
    private static final String ANIMATION_ROOT = "animations/item/";
    private static final String FALLBACK = "abstract_gun";

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem animatable) {
        return Reference.id(MODEL_ROOT + path(animatable) + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem animatable) {
        return GunPaintJobTextures.baseTexture(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem animatable) {
        return Reference.id(ANIMATION_ROOT + path(animatable) + ".animation.json");
    }

    private static String path(AnimatedGunItem item) {
        String path = item.getStats().id().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

}
