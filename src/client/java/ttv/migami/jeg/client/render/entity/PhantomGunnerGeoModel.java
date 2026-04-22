package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerGeoModel extends GeoModel<PhantomGunner> {
    public static final DataTicket<PhantomGunner> ANIMATABLE =
            DataTicket.create("jeg_phantom_gunner_animatable", PhantomGunner.class);

    private static final Identifier MODEL = Reference.id("entity/phantom_gunner");
    private static final Identifier ANIM = Reference.id("entity/phantom_gunner");

    @Override
    public void addAdditionalStateData(PhantomGunner animatable, Object obj, GeoRenderState renderState) {
        renderState.addGeckolibData(ANIMATABLE, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        PhantomGunner animatable = renderState.hasGeckolibData(ANIMATABLE) ? renderState.getGeckolibData(ANIMATABLE) : null;
        if (animatable != null) {
            return animatable.getGeoTexture();
        }
        return Identifier.withDefaultNamespace("textures/entity/phantom.png");
    }

    @Override
    public Identifier getAnimationResource(PhantomGunner animatable) {
        return ANIM;
    }
}

