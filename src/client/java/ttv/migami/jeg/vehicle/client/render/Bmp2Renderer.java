package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Bmp2Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Bmp2Renderer(EntityRendererProvider.Context context) {
        super(context, new Bmp2GeoModel());
    }
}
