package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class Lav150GeoModel extends NamedVehicleGeoModel {
    public Lav150GeoModel() {
        super("lav150");
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
            barrel.setRotX(Mth.clamp(-animatable.turretPitch(partialTick), -15.0F, 32.5F) * Mth.DEG_TO_RAD);
        }
    }
}
