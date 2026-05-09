package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoModel extends GeoModel<VehicleEntity> {
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
}
