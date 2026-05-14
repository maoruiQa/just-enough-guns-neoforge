package ttv.migami.jeg.vehicle.client.render;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class Ah6GeoModel extends NamedVehicleGeoModel {
    public Ah6GeoModel() {
        super("ah6");
    }

    @Override
    public void setCustomAnimations(VehicleEntity animatable, long instanceId, AnimationState<VehicleEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        float partialTick = animationState.getPartialTick();
        float rotorRotation = animatable.propellerRot(partialTick);

        GeoBone mainRotor = this.getAnimationProcessor().getBone("propeller");
        if (mainRotor != null) {
            mainRotor.setRotY(rotorRotation);
        }

        GeoBone tailRotor = this.getAnimationProcessor().getBone("tailPropeller");
        if (tailRotor != null) {
            tailRotor.setRotX(-6.0F * rotorRotation);
        }
    }
}
