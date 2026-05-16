package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class WaveforceTowerRenderer extends AbstractVehicleGeoRenderer {
    public WaveforceTowerRenderer(EntityRendererProvider.Context context) {
        super(context, new WaveforceTowerGeoModel());
    }
}
