package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class Mi28Renderer extends AbstractVehicleGeoRenderer {
    public Mi28Renderer(EntityRendererProvider.Context context) {
        super(context, new Mi28GeoModel());
    }
}
