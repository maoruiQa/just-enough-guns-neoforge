package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class TruckRenderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public TruckRenderer(EntityRendererProvider.Context context) {
        super(context, new TruckGeoModel());
    }
}
