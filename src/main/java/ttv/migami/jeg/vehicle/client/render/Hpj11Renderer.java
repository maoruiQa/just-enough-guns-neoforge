package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Hpj11Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Hpj11Renderer(EntityRendererProvider.Context context) {
        super(context, new Hpj11GeoModel());
    }
}
