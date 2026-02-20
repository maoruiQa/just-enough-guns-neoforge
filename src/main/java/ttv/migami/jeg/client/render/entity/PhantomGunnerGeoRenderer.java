package ttv.migami.jeg.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerGeoRenderer extends GeoEntityRenderer<PhantomGunner> {
    public PhantomGunnerGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomGunnerGeoModel());
        this.shadowRadius = 0.35F;
    }
}

