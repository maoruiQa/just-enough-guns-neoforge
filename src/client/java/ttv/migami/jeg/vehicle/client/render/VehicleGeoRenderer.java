package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class VehicleGeoRenderer extends AbstractVehicleGeoRenderer {
    public VehicleGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new VehicleGeoModel());
    }
}
