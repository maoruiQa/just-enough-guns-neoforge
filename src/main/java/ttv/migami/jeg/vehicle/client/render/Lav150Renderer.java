package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Lav150Renderer extends AbstractVehicleGeoRenderer {
    public Lav150Renderer(EntityRendererProvider.Context context) {
        super(context, new Lav150GeoModel());
    }
}
