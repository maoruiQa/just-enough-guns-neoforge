package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class A10Renderer extends AbstractVehicleGeoRenderer {
    public A10Renderer(EntityRendererProvider.Context context) {
        super(context, new A10GeoModel());
    }
}
