package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleCameraHandler {
    private static final double VEHICLE_ZOOM_DIVISOR = 3.0D;

    private VehicleCameraHandler() {}

    public static double thirdPersonDistance(LocalPlayer player, VehicleEntity vehicle, double currentDistance) {
        CameraPos camera = thirdPersonCameraFor(player, vehicle);
        double configuredDistance = Math.abs(camera.z());
        if (configuredDistance > 0.0D) {
            return configuredDistance;
        }
        return currentDistance;
    }

    public static double adjustFov(double fov) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return fov;
        }
        if (!VehicleClientState.isRidingVehicle() || VehicleClientState.vehicleId() != vehicle.getId() || !VehicleClientState.zoomDown()) {
            return fov;
        }
        return fov / VEHICLE_ZOOM_DIVISOR;
    }

    public static boolean shouldRenderHand() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return true;
        }
        return !VehicleClientState.isRidingVehicle()
                || VehicleClientState.vehicleId() != vehicle.getId()
                || (!((VehicleClientState.freeLookDown() && vehicle.vehicleData().defaults().allowFreeCam())
                || vehicle.shouldBanPassengerHand(player)));
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
}
