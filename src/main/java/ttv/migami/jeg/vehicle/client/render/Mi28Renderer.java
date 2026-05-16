package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;

public final class Mi28Renderer extends AbstractVehicleGeoRenderer<ConfiguredVehicleEntity> {
    public Mi28Renderer(EntityRendererProvider.Context context) {
        super(context, new Mi28GeoModel());
    }
}
