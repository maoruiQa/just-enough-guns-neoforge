package ttv.migami.jeg.vehicle.client;

import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleClientState {
    private static int vehicleId = -1;
    private static boolean freeLookDown;
    private static boolean zoomDown;
    private static boolean seekDown;

    private VehicleClientState() {}

    public static void update(VehicleEntity vehicle, boolean freeLook, boolean zoom, boolean seek) {
        vehicleId = vehicle.getId();
        freeLookDown = freeLook;
        zoomDown = zoom;
        seekDown = seek;
    }

    public static void clear() {
        vehicleId = -1;
        freeLookDown = false;
        zoomDown = false;
        seekDown = false;
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

    public static boolean seekDown() {
        return seekDown;
    }
}
