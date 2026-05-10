package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Hpj11Renderer extends AbstractVehicleGeoRenderer {
    public Hpj11Renderer(EntityRendererProvider.Context context) {
        super(context, new Hpj11GeoModel());
    }
}
