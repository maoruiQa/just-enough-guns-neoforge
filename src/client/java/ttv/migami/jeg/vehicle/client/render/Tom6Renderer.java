package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Tom6Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Tom6Renderer(EntityRendererProvider.Context context) {
        super(context, new Tom6GeoModel());
    }
}
