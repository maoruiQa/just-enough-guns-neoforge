package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerGeoModel extends GeoModel<PhantomGunner> {
    private static final ResourceLocation MODEL = Reference.id("geo/entity/phantom_gunner.geo.json");
    private static final ResourceLocation ANIMATION = Reference.id("animations/entity/phantom_gunner.animation.json");

    @Override
    public ResourceLocation getModelResource(PhantomGunner animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PhantomGunner animatable) {
        return animatable.getGeoTexture();
    }

    @Override
    public ResourceLocation getAnimationResource(PhantomGunner animatable) {
        return ANIMATION;
    }
}
