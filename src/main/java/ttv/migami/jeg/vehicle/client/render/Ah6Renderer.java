package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Ah6Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Ah6Renderer(EntityRendererProvider.Context context) {
        super(context, new Ah6GeoModel());
    }
}
