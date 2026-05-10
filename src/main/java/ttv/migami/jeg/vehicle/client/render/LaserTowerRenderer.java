package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class LaserTowerRenderer extends AbstractVehicleGeoRenderer {
    public LaserTowerRenderer(EntityRendererProvider.Context context) {
        super(context, new LaserTowerGeoModel());
    }
}
