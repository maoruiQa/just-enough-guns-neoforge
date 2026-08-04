package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleSoundHelper;

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
    private static int lastSeekTicks;
    private static boolean lastMissileLocked;
    private static int lastLockedSoundTick = Integer.MIN_VALUE;

    private VehicleClientState() {}

    public static void update(VehicleEntity vehicle, boolean freeLook, boolean zoom, boolean seek) {
        vehicleId = vehicle.getId();
        freeLookDown = freeLook;
        zoomDown = zoom;
        seekDown = seek;
        tickMissileSeekAudio(vehicle, seek);
    }

    private static void tickMissileSeekAudio(VehicleEntity vehicle, boolean seek) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !vehicle.isSelectedVehicleWeaponLockOn(player)) {
            lastSeekTicks = 0;
            lastMissileLocked = false;
            return;
        }
        int seekTicks = vehicle.missileSeekTicks();
        boolean locked = vehicle.hasMissileLock();
        if (seek && seekTicks > 0 && lastSeekTicks <= 0) {
            player.playSound(VehicleSoundHelper.missileLocking(), 2.0F, 1.0F);
        }
        if (locked && !lastMissileLocked) {
            player.playSound(VehicleSoundHelper.missileLocked(), 2.0F, 1.0F);
            lastLockedSoundTick = player.tickCount;
        } else if (locked && seek && player.tickCount - lastLockedSoundTick >= 8) {
            // SW repeats the locked tone while the lock is held.
            player.playSound(VehicleSoundHelper.missileLocked(), 2.0F, 1.0F);
            lastLockedSoundTick = player.tickCount;
        }
        lastSeekTicks = seekTicks;
        lastMissileLocked = locked;
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
        lastSeekTicks = 0;
        lastMissileLocked = false;
        lastLockedSoundTick = Integer.MIN_VALUE;
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
}
