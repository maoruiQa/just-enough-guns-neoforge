package ttv.migami.jeg.entity.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.terror.TerrorPhantom;

public class TerrorPhantomModel extends GeoModel<TerrorPhantom> {

    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/terror_phantom.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/terror_phantom/terror_phantom.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TerrorPhantom animatable) {
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/terror_phantom.animation.json");
    }

    @Override
    public void setCustomAnimations(AnimationState<TerrorPhantom> animationState) {
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            // Hook for future custom rotations if needed
        }
    }
}
