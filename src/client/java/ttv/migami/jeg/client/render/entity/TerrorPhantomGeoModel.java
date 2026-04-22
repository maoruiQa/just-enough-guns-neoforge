package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

public final class TerrorPhantomGeoModel extends GeoModel<AbstractTerrorPhantom> {
    public static final DataTicket<AbstractTerrorPhantom> ANIMATABLE =
            DataTicket.create("jeg_terror_phantom_animatable", AbstractTerrorPhantom.class);

    // GeckoLib v5 resolves these against assets/<modid>/geckolib/(models|animations)/...
    private static final Identifier MODEL = Reference.id("entity/terror_phantom");
    // Do not include ".animation"; GeckoLib appends ".animation.json" internally.
    private static final Identifier ANIM = Reference.id("entity/terror_phantom");
    private static final Identifier FALLBACK_TEXTURE = Identifier.withDefaultNamespace("textures/entity/phantom.png");

    @Override
    public void addAdditionalStateData(AbstractTerrorPhantom animatable, Object obj, GeoRenderState renderState) {
        renderState.addGeckolibData(ANIMATABLE, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        AbstractTerrorPhantom animatable = renderState.hasGeckolibData(ANIMATABLE) ? renderState.getGeckolibData(ANIMATABLE) : null;
        if (animatable != null) {
            return animatable.getGeoTexture();
        }
        return FALLBACK_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(AbstractTerrorPhantom animatable) {
        return ANIM;
    }
}
