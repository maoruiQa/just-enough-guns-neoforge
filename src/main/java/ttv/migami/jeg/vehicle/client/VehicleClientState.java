package ttv.migami.jeg.vehicle.client;

import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleClientState {
    private static int vehicleId = -1;
    private static boolean freeLookDown;

    private VehicleClientState() {}

    public static void update(VehicleEntity vehicle, boolean freeLook) {
        vehicleId = vehicle.getId();
        freeLookDown = freeLook;
    }

    public static void clear() {
        vehicleId = -1;
        freeLookDown = false;
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
}
