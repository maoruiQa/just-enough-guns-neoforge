package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class LaserTowerRenderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public LaserTowerRenderer(EntityRendererProvider.Context context) {
        super(context, new LaserTowerGeoModel());
    }
}
