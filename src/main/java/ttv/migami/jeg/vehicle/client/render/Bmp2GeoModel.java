package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class Bmp2GeoModel extends NamedVehicleGeoModel {
    public Bmp2GeoModel() {
        super("bmp2");
    }

    @Override
    public void setCustomAnimations(VehicleEntity animatable, long instanceId, AnimationState<VehicleEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        float partialTick = animationState.getPartialTick();

        GeoBone turret = this.getAnimationProcessor().getBone("turret");
        if (turret != null) {
            turret.setRotY(animatable.turretYaw(partialTick) * Mth.DEG_TO_RAD);
        }

        GeoBone barrel = this.getAnimationProcessor().getBone("barrel");
        if (barrel != null) {
            barrel.setRotX(Mth.clamp(-animatable.turretPitch(partialTick), -7.5F, 74.0F) * Mth.DEG_TO_RAD);
        }
    }
}
