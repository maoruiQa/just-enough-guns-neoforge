package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.vehicle.client.screen.VehicleScreen;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;

public final class VehicleInputHandler {
    private static final int VEHICLE_INVENTORY_INPUT_BLOCK_TICKS = 4;
    private static boolean suppressPlayerInventoryClick;
    private static boolean suppressVehicleInventoryClick;
    private static boolean ignorePlayerInventoryUntilRelease;
    private static boolean ignoreVehicleInventoryUntilRelease;
    private static boolean vehicleInventoryWasDown;
    private static int vehicleInventoryInputBlockTicks;
    private static int scrollWeaponDirection;
    private static int scrollCooldown;

    private VehicleInputHandler() {}

    public static void onClientTick(Minecraft minecraft) {
        if (VehicleNetworkCheck.tick(minecraft)) return;
        if (VehicleCaptureCheck.tick(minecraft)) return;
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            scrollWeaponDirection = scrollCooldown = 0;
            VehicleClientState.clear();
            ignorePlayerInventoryUntilRelease = false;
            ignoreVehicleInventoryUntilRelease = false;
            vehicleInventoryWasDown = false;
            vehicleInventoryInputBlockTicks = 0;
            return;
        }

        if (scrollCooldown > 0) scrollCooldown--;
        boolean vehicleInventoryDown = minecraft.options.keyInventory.isDown();
        if (vehicleInventoryInputBlockTicks > 0) {
            vehicleInventoryInputBlockTicks--;
            while (minecraft.options.keyInventory.consumeClick()) {
            }
        }
        if (ignoreVehicleInventoryUntilRelease && !vehicleInventoryDown) {
            ignoreVehicleInventoryUntilRelease = false;
        }
        if (ignorePlayerInventoryUntilRelease && !KeyBindings.VEHICLE_PLAYER_INVENTORY.isDown()) {
            ignorePlayerInventoryUntilRelease = false;
        }

        boolean controlsActive = minecraft.screen == null && minecraft.isWindowActive();
        boolean freeLook = controlsActive && KeyBindings.VEHICLE_FREE_LOOK.isDown();
        boolean vehicleZoom = controlsActive && minecraft.options.keyUse.isDown() && vehicle.canPassengerUseSelectedVehicleWeapon(player);
        VehicleClientState.update(vehicle, freeLook, vehicleZoom, controlsActive && KeyBindings.VEHICLE_SEEK.isDown());
        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        if (!controlsActive) {
            VehicleClientState.syncMousePosition(mouseX, mouseY);
        } else {
            VehicleClientState.setMousePosition(mouseX, mouseY);
        }
        boolean seek = controlsActive && KeyBindings.VEHICLE_SEEK.isDown();
        boolean aircraftControls = isAircraftDriver(player, vehicle);
        if (aircraftControls && controlsActive) {
            var engine = vehicle.vehicleData().defaults().engine();
            VehicleClientState.updateAircraftMouse(vehicle.usesSwControls() ? (float) engine.mouseSensitivity() : .1F, vehicle.usesSwControls() ? (float) engine.mouseSpeedX() : .5F, vehicle.usesSwControls() ? (float) engine.mouseSpeedY() : .35F, minecraft.options.invertYMouse().get(), freeLook);
        }
        float flightX = VehicleClientState.mouseLerpX(), flightY = VehicleClientState.mouseLerpY();
        if (vehicle.usesSwControls() && minecraft.options.getCameraType().isFirstPerson()) {
            float roll = vehicle.roll();
            float fraction = Math.abs(roll) / 90.0F;
            float sign = roll < 0 ? 1 : roll > 0 ? -1 : 0;
            if (Math.abs(roll) > 90) sign *= 1 - (Math.abs(roll) - 90) / 90;
            flightX = (1 - fraction) * VehicleClientState.mouseLerpX() + fraction * VehicleClientState.mouseLerpY() * sign;
            flightY = (1 - fraction) * VehicleClientState.mouseLerpY() + fraction * VehicleClientState.mouseLerpX() * (roll < 0 ? -1 : 1);
        }
        boolean reload = KeyBindings.RELOAD.consumeClick();
        int weaponSlot = -1;
        for (int index = 0; index < minecraft.options.keyHotbarSlots.length; index++) {
            if (minecraft.options.keyHotbarSlots[index].consumeClick()) {
                weaponSlot = vehicle.vehicleWeaponIndexForDisplaySlot(player, index);
                break;
            }
        }
        VehicleClientState.update(vehicle, freeLook, vehicleZoom, seek);
        if (KeyBindings.VEHICLE_CHANGE_SEAT.consumeClick()) {
            ClientNetworkHandler.sendVehicleChangeSeat(vehicle.getId());
        }
        if (KeyBindings.VEHICLE_DISMOUNT.consumeClick()) {
            vehicle.clearClientControlState();
            VehicleClientState.clear();
            ClientNetworkHandler.sendVehicleDismount(vehicle.getId());
            return;
        }
        boolean vehicleInventoryClick = !ignoreVehicleInventoryUntilRelease
                && vehicleInventoryInputBlockTicks <= 0
                && (minecraft.options.keyInventory.consumeClick() || (vehicleInventoryDown && !vehicleInventoryWasDown));
        vehicleInventoryWasDown = vehicleInventoryDown;
        if (suppressVehicleInventoryClick) {
            suppressVehicleInventoryClick = false;
            vehicleInventoryClick = false;
        }
        if (vehicleInventoryClick) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            if (minecraft.screen instanceof VehicleScreen) {
                player.closeContainer();
                minecraft.setScreen(null);
                clearPendingVehicleInventoryClicks();
            } else if (minecraft.screen == null) {
                ClientNetworkHandler.sendVehicleOpenMenu(vehicle.getId());
                clearPendingVehicleInventoryClicks();
            }
        }
        boolean playerInventoryClick = !ignorePlayerInventoryUntilRelease && KeyBindings.VEHICLE_PLAYER_INVENTORY.consumeClick();
        if (suppressPlayerInventoryClick) {
            suppressPlayerInventoryClick = false;
            playerInventoryClick = false;
        }
        if (playerInventoryClick) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            if (minecraft.screen instanceof InventoryScreen) {
                player.closeContainer();
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new InventoryScreen(player));
            }
        }
        VehicleInput input = minecraft.screen != null || !minecraft.isWindowActive() ? VehicleInput.EMPTY : new VehicleInput(
                minecraft.options.keyUp.isDown(),
                minecraft.options.keyDown.isDown(),
                minecraft.options.keyLeft.isDown(),
                minecraft.options.keyRight.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                minecraft.options.keyJump.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                minecraft.options.keyAttack.isDown(),
                reload,
                freeLook,
                KeyBindings.VEHICLE_SWITCH_WEAPON.consumeClick() || scrollWeaponDirection > 0,
                KeyBindings.VEHICLE_PREVIOUS_WEAPON.consumeClick() || scrollWeaponDirection < 0,
                weaponSlot,
                seek,
                KeyBindings.VEHICLE_DEPLOY_DECOY.consumeClick(),
                aircraftControls && !freeLook ? flightX : 0.0F,
                aircraftControls && !freeLook ? flightY : 0.0F,
                vehicleZoom
        );
        scrollWeaponDirection = 0;
        vehicle.processClientInput(player, input);
        ClientNetworkHandler.sendVehicleInput(vehicle.getId(), input);
    }

    public static double adjustMouseSensitivity(double base) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return base;
        }
        if (usesHandheldMouse(player, vehicle)) return base;
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0 || seatIndex >= vehicle.vehicleData().defaults().seats().size()) {
            return base;
        }
        var seat = vehicle.vehicleData().defaults().seats().get(seatIndex);
        if (minecraft.options.getCameraType().isFirstPerson()) {
            clampSeatView(player, vehicle, seat);
        }
        float deltaX = VehicleClientState.mouseDeltaX();
        float deltaY = VehicleClientState.mouseDeltaY();
        if (deltaX == 0.0F && deltaY == 0.0F) {
            deltaX = Mth.wrapDegrees(player.getYRot() - player.yRotO);
            deltaY = player.getXRot() - player.xRotO;
        }
        VehicleClientState.setMouseDelta(deltaX, deltaY);
        float scaledBase = (float) base;
        float sensitivity = minecraft.options.getCameraType().isFirstPerson() ? seat.sensitivityY() : seat.sensitivityZ();
        if (VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown()) {
            sensitivity = seat.sensitivityX();
        }
        return vehicle.usesSwControls() ? Math.max(0.0D, base * sensitivity) : Math.max(0.0F, scaledBase * sensitivity);
    }

    public static boolean handleVehicleMouseTurn(Minecraft minecraft, double accumulatedDX, double accumulatedDY, double frameTime) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return false;
        }
        if (usesHandheldMouse(player, vehicle)) return false;
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0 || seatIndex >= vehicle.vehicleData().defaults().seats().size()) {
            return false;
        }

        double sensitivitySetting = minecraft.options.sensitivity().get() * 0.6000000238418579D + 0.20000000298023224D;
        double baseSensitivity = sensitivitySetting * sensitivitySetting * sensitivitySetting * 8.0D;
        double adjustedSetting = adjustMouseSensitivity(sensitivitySetting);
        double vehicleSensitivity = vehicle.usesSwControls() ? adjustedSetting * adjustedSetting * adjustedSetting * 8.0D : adjustMouseSensitivity(baseSensitivity);
        double turnX = accumulatedDX * vehicleSensitivity;
        double turnY = accumulatedDY * vehicleSensitivity;
        minecraft.getTutorial().onMouse(turnX, turnY);
        player.turn(turnX, turnY * (minecraft.options.invertYMouse().get() ? -1 : 1));

        var seat = vehicle.vehicleData().defaults().seats().get(seatIndex);
        clampSeatView(player, vehicle, seat);
        vehicle.refreshClientTurretAim(player);
        return true;
    }

    public static void checkHandheldCapture(Minecraft mc, boolean aiming) {
        var player = mc.player;
        if (!(player.getMainHandItem().getItem() instanceof ttv.migami.jeg.item.GunItem)
                || !(player.getVehicle() instanceof VehicleEntity vehicle) || vehicle.shouldBanPassengerHand(player)) {
            throw new IllegalStateException("Truck must expose the native held gun and its HUD");
        }
        if (handleVehicleMouseTurn(mc, 0, 0, 0) || adjustMouseSensitivity(.5) != .5) {
            throw new IllegalStateException("Truck held gun must retain native mouse input");
        }
        float ads = ttv.migami.jeg.client.handler.AimingHandler.get().getNormalisedAdsProgress();
        if (aiming ? ads < .99F : ads > .01F) throw new IllegalStateException("Native gun ADS did not transition: " + ads);
    }

    private static boolean usesHandheldMouse(LocalPlayer player, VehicleEntity vehicle) {
        return vehicle.usesSwControls() && !vehicle.shouldBanPassengerHand(player)
                && player.getMainHandItem().getItem() instanceof ttv.migami.jeg.item.GunItem;
    }

    private static void clampSeatView(LocalPlayer player, VehicleEntity vehicle, ttv.migami.jeg.vehicle.data.subdata.SeatInfo seat) {
        if (vehicle.usesSwControls()) {
            if (isAircraftDriver(player, vehicle) && seat.sensitivityY() == 0 && seat.sensitivityZ() == 0 && !VehicleClientState.freeLookDown()) {
                player.setYRot(vehicle.getYRot());
                player.setXRot(vehicle.getXRot());
            }
            vehicle.clampSwPassengerView(player);
            return;
        }
        float targetYaw = aircraftBoundYaw(player, vehicle, seat);
        float targetPitch = aircraftBoundPitch(player, vehicle, seat);
        player.setYRot(targetYaw);
        player.yRotO = targetYaw;
        player.yHeadRot = targetYaw;
        player.yHeadRotO = targetYaw;
        player.yBodyRot = targetYaw;
        player.yBodyRotO = targetYaw;
        player.setXRot(targetPitch);
        player.xRotO = targetPitch;
    }

    private static float aircraftBoundYaw(LocalPlayer player, VehicleEntity vehicle, ttv.migami.jeg.vehicle.data.subdata.SeatInfo seat) {
        if (isAircraftDriver(player, vehicle) && seat.sensitivityY() == 0.0F && seat.sensitivityZ() == 0.0F && !VehicleClientState.freeLookDown()) {
            return vehicle.getYRot();
        }
        float relativeYaw = Mth.wrapDegrees(player.getYRot() - vehicle.getYRot() - (vehicle.usesSwControls() ? seat.orientation() : 0));
        float clampedYaw = Mth.clamp(relativeYaw, seat.minYaw(), seat.maxYaw());
        return vehicle.getYRot() + (vehicle.usesSwControls() ? seat.orientation() : 0) + clampedYaw;
    }

    private static float aircraftBoundPitch(LocalPlayer player, VehicleEntity vehicle, ttv.migami.jeg.vehicle.data.subdata.SeatInfo seat) {
        if (isAircraftDriver(player, vehicle) && seat.sensitivityY() == 0.0F && seat.sensitivityZ() == 0.0F && !VehicleClientState.freeLookDown()) {
            return Mth.clamp(vehicle.getXRot(), seat.minPitch(), seat.maxPitch());
        }
        return vehicle.usesSwControls() ? Mth.clamp(player.getXRot(), -seat.maxPitch(), -seat.minPitch()) : Mth.clamp(player.getXRot(), seat.minPitch(), seat.maxPitch());
    }

    private static boolean isAircraftDriver(LocalPlayer player, VehicleEntity vehicle) {
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0 || seatIndex >= vehicle.vehicleData().defaults().seats().size()) {
            return false;
        }
        var seat = vehicle.vehicleData().defaults().seats().get(seatIndex);
        var type = vehicle.vehicleData().defaults().vehicleType();
        return seat.driver() && (type == ttv.migami.jeg.vehicle.data.subdata.VehicleType.HELICOPTER || type == ttv.migami.jeg.vehicle.data.subdata.VehicleType.AIRCRAFT);
    }

    public static boolean onScreenKeyPressed(int keyCode, int scanCode) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle) || !(minecraft.screen instanceof InventoryScreen)) {
            return true;
        }
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            ClientNetworkHandler.sendVehicleOpenMenu(vehicle.getId());
            clearPendingVehicleInventoryClicks();
            return false;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(keyCode, scanCode)) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            minecraft.setScreen(null);
            clearPendingPlayerInventoryClicks();
            return false;
        }
        return true;
    }

    public static void syncMouseToCurrentCursor() {
        Minecraft minecraft = Minecraft.getInstance();
        VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
    }

    public static void clearPendingPlayerInventoryClicks() {
        while (KeyBindings.VEHICLE_PLAYER_INVENTORY.consumeClick()) {
        }
        suppressPlayerInventoryClick = true;
        ignorePlayerInventoryUntilRelease = true;
    }

    public static void clearPendingVehicleInventoryClicks() {
        Minecraft minecraft = Minecraft.getInstance();
        while (minecraft.options.keyInventory.consumeClick()) {
        }
        suppressVehicleInventoryClick = true;
        ignoreVehicleInventoryUntilRelease = true;
        vehicleInventoryWasDown = minecraft.options.keyInventory.isDown();
        vehicleInventoryInputBlockTicks = VEHICLE_INVENTORY_INPUT_BLOCK_TICKS;
    }

    public static boolean shouldIgnorePlayerInventoryKey() {
        return ignorePlayerInventoryUntilRelease;
    }

    public static boolean shouldIgnoreVehicleInventoryKey() {
        return ignoreVehicleInventoryUntilRelease || vehicleInventoryInputBlockTicks > 0;
    }

    public static void suppressPlayerInventoryClickOnce() {
        suppressPlayerInventoryClick = true;
    }

    public static void suppressVehicleInventoryClickOnce() {
        suppressVehicleInventoryClick = true;
    }
    public static boolean onVehicleScroll(double amount) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || !(player.getVehicle() instanceof VehicleEntity vehicle) || !vehicle.usesSwControls()) return false;
        if (vehicle.getControllingPassenger() == player && VehicleClientState.freeLookDown()) {
            VehicleClientState.scrollCamera(amount);
            return true;
        } else if (!player.isShiftKeyDown() && vehicle.shouldBanPassengerHand(player) && vehicle.canPassengerUseSelectedVehicleWeapon(player)) {
            if (scrollCooldown == 0) {
                scrollWeaponDirection = amount > 0 ? -1 : 1;
                scrollCooldown = 3;
            }
            return true;
        }
        return false;
    }
}
