package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class Lav150GeoModel extends GeoModel<VehicleEntity> {
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

    @Override
    public void setCustomAnimations(VehicleEntity animatable, long instanceId, AnimationState<VehicleEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        GeoBone turret = this.getAnimationProcessor().getBone("turret");
        if (turret != null) {
            turret.setRotY(animatable.turretYaw() * Mth.DEG_TO_RAD);
        }

        GeoBone barrel = this.getAnimationProcessor().getBone("barrel");
        if (barrel != null) {
            barrel.setRotX(Mth.clamp(-animatable.turretPitch(), -15.0F, 32.5F) * Mth.DEG_TO_RAD);
        }
    }
}
