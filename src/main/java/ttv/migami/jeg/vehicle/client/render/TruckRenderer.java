package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class TruckRenderer extends AbstractVehicleGeoRenderer {
    public TruckRenderer(EntityRendererProvider.Context context) {
        super(context, new TruckGeoModel());
    }
}
