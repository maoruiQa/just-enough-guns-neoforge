package ttv.migami.jeg.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

public final class TerrorPhantomGeoRenderer extends GeoEntityRenderer<AbstractTerrorPhantom> {
    public TerrorPhantomGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new TerrorPhantomGeoModel());
        this.shadowRadius = 0.75F;
    }
}

