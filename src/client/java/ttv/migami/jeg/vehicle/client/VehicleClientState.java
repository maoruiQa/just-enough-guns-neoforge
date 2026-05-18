package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleClientState {
    private static int vehicleId = -1;
    private static boolean freeLookDown;
    private static boolean zoomDown;
    private static boolean seekDown;
    private static float mouseDeltaX;
    private static float mouseDeltaY;
    private static float mouseLerpX;
    private static float mouseLerpY;
    private static double lastMouseX = Double.NaN;
    private static double lastMouseY = Double.NaN;

    private VehicleClientState() {}

    public static void update(VehicleEntity vehicle, boolean freeLook, boolean zoom, boolean seek) {
        vehicleId = vehicle.getId();
        freeLookDown = freeLook;
        zoomDown = zoom;
        seekDown = seek;
    }

    public static void setMousePosition(double mouseX, double mouseY) {
        if (!Double.isNaN(lastMouseX) && !Double.isNaN(lastMouseY)) {
            mouseDeltaX = (float) (mouseX - lastMouseX);
            mouseDeltaY = (float) (mouseY - lastMouseY);
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public static void updateAircraftMouse(float sensitivity, float smoothingX, float smoothingY, boolean invertY, boolean freeLook) {
        float speedX = mouseDeltaX * sensitivity;
        float speedY = mouseDeltaY * sensitivity * (invertY ? -1.0F : 1.0F);
        mouseLerpX = Mth.lerp(smoothingX, mouseLerpX, speedX);
        mouseLerpY = Mth.lerp(smoothingY, mouseLerpY, speedY);
        if (freeLook) {
            mouseLerpX = 0.0F;
            mouseLerpY = 0.0F;
        }
    }

    public static void setMouseDelta(float deltaX, float deltaY) {
        mouseDeltaX = deltaX;
        mouseDeltaY = deltaY;
    }

    public static void clearFrameDeltas() {
        mouseDeltaX = 0.0F;
        mouseDeltaY = 0.0F;
    }

    public static void syncMousePosition(double mouseX, double mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        clearFrameDeltas();
        mouseLerpX = 0.0F;
        mouseLerpY = 0.0F;
    }

    public static void clear() {
        vehicleId = -1;
        freeLookDown = false;
        zoomDown = false;
        seekDown = false;
        lastMouseX = Double.NaN;
        lastMouseY = Double.NaN;
        clearFrameDeltas();
        mouseLerpX = 0.0F;
        mouseLerpY = 0.0F;
    }

    public static boolean isRidingVehicle() {
        return vehicleId >= 0 || currentVehicle() != null;
    }

    public static int vehicleId() {
        VehicleEntity vehicle = currentVehicle();
        return vehicleId >= 0 ? vehicleId : vehicle == null ? -1 : vehicle.getId();
    }

    public static boolean freeLookDown() {
        return freeLookDown;
    }

    public static boolean zoomDown() {
        if (zoomDown) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null
                && player.getVehicle() instanceof VehicleEntity vehicle
                && minecraft.options.keyUse.isDown()
                && vehicle.canPassengerUseSelectedVehicleWeapon(player);
    }

    public static boolean seekDown() {
        return seekDown;
    }

    public static float mouseDeltaX() {
        return mouseDeltaX;
    }

    public static float mouseDeltaY() {
        return mouseDeltaY;
    }

    public static float mouseLerpX() {
        return mouseLerpX;
    }

    public static float mouseLerpY() {
        return mouseLerpY;
    }

    private static VehicleEntity currentVehicle() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof VehicleEntity vehicle ? vehicle : null;
    }
}
