package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class A10Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public A10Renderer(EntityRendererProvider.Context context) {
        super(context, new A10GeoModel());
    }
}
