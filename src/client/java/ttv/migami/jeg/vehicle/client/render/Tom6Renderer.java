package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Tom6Renderer extends AbstractVehicleGeoRenderer {
    public Tom6Renderer(EntityRendererProvider.Context context) {
        super(context, new Tom6GeoModel());
    }
}
