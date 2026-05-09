package ttv.migami.jeg.vehicle.client;

import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleClientState {
    private static int vehicleId = -1;
    private static boolean freeLookDown;
    private static boolean zoomDown;

    private VehicleClientState() {}

    public static void update(VehicleEntity vehicle, boolean freeLook, boolean zoom) {
        vehicleId = vehicle.getId();
        freeLookDown = freeLook;
        zoomDown = zoom;
    }

    public static void clear() {
        vehicleId = -1;
        freeLookDown = false;
        zoomDown = false;
    }

    public static boolean isRidingVehicle() {
        return vehicleId >= 0;
    }

    public static int vehicleId() {
        return vehicleId;
    }

    public static boolean freeLookDown() {
        return freeLookDown;
    }

    public static boolean zoomDown() {
        return zoomDown;
    }
}
