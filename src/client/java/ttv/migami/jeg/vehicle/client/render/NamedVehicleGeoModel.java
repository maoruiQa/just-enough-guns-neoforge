package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

class NamedVehicleGeoModel extends GeoModel<VehicleEntity> {
    private final String vehicleId;

    protected NamedVehicleGeoModel(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    @Override
    public ResourceLocation getModelResource(VehicleEntity animatable) {
        return DefaultVehicleResource.model(animatable);
    }

    @Override
    public ResourceLocation getTextureResource(VehicleEntity animatable) {
        return DefaultVehicleResource.texture(animatable);
    }

    @Override
    public ResourceLocation getAnimationResource(VehicleEntity animatable) {
        return DefaultVehicleResource.animation(animatable);
    }

    protected boolean matches(VehicleEntity vehicle) {
        return this.vehicleId.equals(vehicle.vehicleDataId().getPath());
    }
}
