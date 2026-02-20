package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

public final class TerrorPhantomGeoModel extends GeoModel<AbstractTerrorPhantom> {
    private static final ResourceLocation MODEL = Reference.id("geo/entity/terror_phantom.geo.json");
    private static final ResourceLocation ANIMATION = Reference.id("animations/entity/terror_phantom.animation.json");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    @Override
    public ResourceLocation getModelResource(AbstractTerrorPhantom animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractTerrorPhantom animatable) {
        ResourceLocation tex = animatable.getGeoTexture();
        return tex != null ? tex : FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AbstractTerrorPhantom animatable) {
        return ANIMATION;
    }
}
