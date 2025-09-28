package ttv.migami.jeg.entity.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.processing.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.gunner.PhantomGunner;
import ttv.migami.jeg.client.render.gun.JegDataTickets;

public class PhantomGunnerModel extends GeoModel<PhantomGunner> {

    @Override
    public ResourceLocation getModelResource(GeoRenderState renderState) {
        return new ResourceLocation(Reference.MOD_ID, "geo/entity/phantom_gunner.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeoRenderState renderState) {
        PhantomGunner gunner = renderState.getOrDefaultGeckolibData(JegDataTickets.PHANTOM_GUNNER, null);
        if (gunner != null && gunner.isPlayerOwned()) {
            return new ResourceLocation(Reference.MOD_ID, "textures/entity/phantom_gunner/phantom_gunner_friendly.png");
        }
        return new ResourceLocation(Reference.MOD_ID, "textures/entity/phantom_gunner/phantom_gunner.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PhantomGunner animatable) {
        return new ResourceLocation(Reference.MOD_ID, "animations/entity/phantom_gunner.animation.json");
    }

    @Override
    public void setCustomAnimations(AnimationState<PhantomGunner> animationState) {
        GeoBone root = getAnimationProcessor().getBone("root");
        if (root != null) {
            // Hook for future animation adjustments if needed
        }
    }
}
