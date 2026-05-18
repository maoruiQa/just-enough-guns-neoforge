package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleCameraHandler {
    private static final float VEHICLE_ZOOM_DIVISOR = 3.0F;

    private VehicleCameraHandler() {}

    public static float adjustFov(float fov) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return fov;
        }
        if (!VehicleClientState.isRidingVehicle()
                || VehicleClientState.vehicleId() != vehicle.getId()
                || !VehicleClientState.zoomDown()) {
            return fov;
        }
        return fov / VEHICLE_ZOOM_DIVISOR;
    }

    public static float thirdPersonCameraDistance(float fallbackDistance) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return fallbackDistance;
        }
        if (!VehicleClientState.isRidingVehicle() || VehicleClientState.vehicleId() != vehicle.getId()) {
            return fallbackDistance;
        }
        double configuredDistance = Math.abs(thirdPersonCameraFor(player, vehicle).z());
        return configuredDistance > 0.0D ? (float) configuredDistance : fallbackDistance;
    }

    public static void applyThirdPersonCameraOffset(net.minecraft.client.Camera camera) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || camera.entity() != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!VehicleClientState.isRidingVehicle()
                || VehicleClientState.vehicleId() != vehicle.getId()
                || Minecraft.getInstance().options.getCameraType().isFirstPerson()
                || vehicle.usesVehiclePoseTransform()) {
            return;
        }
        CameraPos cameraPos = thirdPersonCameraFor(player, vehicle);
        if (cameraPos.x() != 0.0D || cameraPos.y() != 0.0D) {
            ((VehicleCameraAccess) camera).jeg$moveVehicleCamera(0.0F, (float) cameraPos.y(), (float) -cameraPos.x());
        }
    }

    public static boolean shouldHideHand() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return false;
        }
        return VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && ((VehicleClientState.freeLookDown() && vehicle.vehicleData().defaults().allowFreeCam())
                || vehicle.shouldBanPassengerHand(player));
    }

    private static CameraPos thirdPersonCameraFor(LocalPlayer player, VehicleEntity vehicle) {
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex >= 0 && seatIndex < vehicle.vehicleData().defaults().seats().size()) {
            CameraPos seatCamera = vehicle.vehicleData().defaults().seats().get(seatIndex).zoomCamera();
            if (seatCamera.useAircraftCamera()) {
                return new CameraPos(seatCamera.aircraftX(), seatCamera.aircraftY(), seatCamera.aircraftZ());
            }
        }
        return vehicle.vehicleData().defaults().thirdPersonCamera();
    }

    public interface VehicleCameraAccess {
        void jeg$moveVehicleCamera(float x, float y, float z);
    }
}
