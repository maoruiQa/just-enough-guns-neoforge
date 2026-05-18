package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Lav150Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Lav150Renderer(EntityRendererProvider.Context context) {
        super(context, new Lav150GeoModel());
    }
}
