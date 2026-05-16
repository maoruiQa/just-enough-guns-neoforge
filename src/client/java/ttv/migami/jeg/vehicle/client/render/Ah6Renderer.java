package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Ah6Renderer extends AbstractVehicleGeoRenderer {
    public Ah6Renderer(EntityRendererProvider.Context context) {
        super(context, new Ah6GeoModel());
    }
}
