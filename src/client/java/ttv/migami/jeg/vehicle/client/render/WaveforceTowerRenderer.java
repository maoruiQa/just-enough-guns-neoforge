package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class WaveforceTowerRenderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public WaveforceTowerRenderer(EntityRendererProvider.Context context) {
        super(context, new WaveforceTowerGeoModel());
    }
}
