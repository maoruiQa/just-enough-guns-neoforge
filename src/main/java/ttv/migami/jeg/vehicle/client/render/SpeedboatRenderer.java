package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class SpeedboatRenderer extends AbstractVehicleGeoRenderer {
    public SpeedboatRenderer(EntityRendererProvider.Context context) {
        super(context, new SpeedboatGeoModel());
    }
}
