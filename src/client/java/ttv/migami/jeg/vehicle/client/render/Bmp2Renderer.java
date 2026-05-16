package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Bmp2Renderer extends AbstractVehicleGeoRenderer {
    public Bmp2Renderer(EntityRendererProvider.Context context) {
        super(context, new Bmp2GeoModel());
    }
}
