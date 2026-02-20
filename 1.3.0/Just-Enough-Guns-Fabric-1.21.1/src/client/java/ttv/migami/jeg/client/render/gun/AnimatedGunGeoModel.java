package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunGeoModel extends GeoModel<AnimatedGunItem> {
    private static final String FALLBACK_GUN = "abstract_gun";

    private static ResourceLocation modelPath(String gunId) {
        return Reference.id("geo/item/gun/" + gunId + ".geo.json");
    }

    private static ResourceLocation texturePath(String gunId) {
        return Reference.id("textures/animated/gun/" + gunId + ".png");
    }

    private static ResourceLocation animationPath(String gunId) {
        return Reference.id("animations/item/" + gunId + ".animation.json");
    }

    private static String gunId(AnimatedGunItem animatable) {
        if (animatable == null || animatable.getStats() == null || animatable.getStats().id() == null) {
            return FALLBACK_GUN;
        }
        return animatable.getStats().id().getPath();
    }

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem animatable) {
        return modelPath(gunId(animatable));
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem animatable) {
        return texturePath(gunId(animatable));
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem animatable) {
        return animationPath(gunId(animatable));
    }
}
