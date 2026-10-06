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

    @Override
    public void setCustomAnimations(VehicleEntity vehicle, long instanceId, software.bernie.geckolib.animation.AnimationState<VehicleEntity> state) {
        super.setCustomAnimations(vehicle, instanceId, state);
        if (!vehicle.usesSwControls()) return;
        float tick = state.getPartialTick();
        for (var bone : getAnimationProcessor().getRegisteredBones()) {
            String name = bone.getName();
            if (name.startsWith("wheelL") || name.startsWith("wheelR")) {
                bone.setRotX(1.5F * vehicle.swWheelRotation(name.startsWith("wheelL"), tick));
                if (name.endsWith("Turn")) bone.setRotY(vehicle.swRudderRotation(tick));
            } else if (name.equals("control") && vehicle.isTruckVehicle()) bone.setRotY(12 * vehicle.swRudderRotation(tick));
            else if (vehicle.vehicleDataId().getPath().equals("bmp2") && (name.startsWith("trackMov") || name.startsWith("trackRot"))) {
                float phase = vehicle.swTrackPhase(name.charAt(8) == 'L', Integer.parseInt(name.substring(9)), tick);
                if (name.startsWith("trackRot")) bone.setRotX(-Bmp2GeoModel.swTrackRotX(phase) * net.minecraft.util.Mth.DEG_TO_RAD);
                else { bone.setPosY(Bmp2GeoModel.swTrackY(phase)); bone.setPosZ(Bmp2GeoModel.swTrackZ(phase)); }
            }
        }
    }

    protected boolean matches(VehicleEntity vehicle) {
        return this.vehicleId.equals(vehicle.vehicleDataId().getPath());
    }
}
