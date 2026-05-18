package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoRenderer<T extends VehicleEntity> extends AbstractVehicleGeoRenderer<T> {
    public VehicleGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new VehicleGeoModel());
    }
}
