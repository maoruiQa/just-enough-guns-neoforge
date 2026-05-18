package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class SpeedboatRenderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public SpeedboatRenderer(EntityRendererProvider.Context context) {
        super(context, new SpeedboatGeoModel());
    }
}
