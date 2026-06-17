package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.client.screen.VehicleScreen;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;
import ttv.migami.jeg.vehicle.network.VehicleInputPayload;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleInputHandler {
    private static final int VEHICLE_INVENTORY_INPUT_BLOCK_TICKS = 4;
    private static boolean suppressPlayerInventoryClick;
    private static boolean suppressVehicleInventoryClick;
    private static boolean ignorePlayerInventoryUntilRelease;
    private static boolean ignoreVehicleInventoryUntilRelease;
    private static boolean vehicleInventoryWasDown;
    private static int vehicleInventoryInputBlockTicks;

    private VehicleInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            VehicleClientState.clear();
            ignorePlayerInventoryUntilRelease = false;
            ignoreVehicleInventoryUntilRelease = false;
            vehicleInventoryWasDown = false;
            vehicleInventoryInputBlockTicks = 0;
            return;
        }

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

        double mouseX = minecraft.mouseHandler.xpos();
        double mouseY = minecraft.mouseHandler.ypos();
        Screen screen = minecraft.gui.screen();
        if (screen != null) {
            VehicleClientState.syncMousePosition(mouseX, mouseY);
        } else {
            VehicleClientState.setMousePosition(mouseX, mouseY);
        }
        boolean freeLook = KeyBindings.VEHICLE_FREE_LOOK.isDown();
        boolean seek = KeyBindings.VEHICLE_SEEK.isDown();
        boolean aircraftControls = isAircraftDriver(player, vehicle);
        if (aircraftControls && screen == null) {
            VehicleClientState.updateAircraftMouse(0.1F, 0.5F, 0.35F, false, freeLook);
        }
        boolean reload = KeyBindings.RELOAD.consumeClick();
        int weaponSlot = -1;
        for (int index = 0; index < minecraft.options.keyHotbarSlots.length; index++) {
            if (minecraft.options.keyHotbarSlots[index].consumeClick()) {
                weaponSlot = vehicle.vehicleWeaponIndexForDisplaySlot(player, index);
                break;
            }
        }
        boolean vehicleZoom = minecraft.options.keyUse.isDown() && vehicle.canPassengerUseSelectedVehicleWeapon(player);
        VehicleClientState.update(vehicle, freeLook, vehicleZoom, seek);
        if (KeyBindings.VEHICLE_CHANGE_SEAT.consumeClick()) {
            NetworkHandler.sendVehicleChangeSeat(vehicle.getId());
        }
        if (KeyBindings.VEHICLE_DISMOUNT.consumeClick()) {
            vehicle.clearClientControlState();
            VehicleClientState.clear();
            NetworkHandler.sendVehicleDismount(vehicle.getId());
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
            if (screen instanceof VehicleScreen) {
                player.closeContainer();
                minecraft.gui.setScreen(null);
                clearPendingVehicleInventoryClicks();
            } else if (screen == null) {
                NetworkHandler.sendVehicleOpenMenu(vehicle.getId());
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
            if (screen instanceof InventoryScreen) {
                player.closeContainer();
                minecraft.gui.setScreen(null);
            } else if (screen == null) {
                minecraft.gui.setScreen(new InventoryScreen(player));
            }
        }
        VehicleInput input = new VehicleInput(
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
                KeyBindings.VEHICLE_SWITCH_WEAPON.consumeClick(),
                KeyBindings.VEHICLE_PREVIOUS_WEAPON.consumeClick(),
                weaponSlot,
                seek,
                KeyBindings.VEHICLE_DEPLOY_DECOY.consumeClick(),
                aircraftControls && !freeLook ? VehicleClientState.mouseLerpX() : 0.0F,
                aircraftControls && !freeLook ? VehicleClientState.mouseLerpY() : 0.0F
        );
        vehicle.processClientInput(player, input);
        minecraft.getConnection().send(new VehicleInputPayload(
                vehicle.getId(),
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.brake(),
                input.ascend(),
                input.descend(),
                input.fire(),
                input.reload(),
                input.freeLook(),
                input.switchWeapon(),
                input.previousWeapon(),
                input.weaponSlot(),
                input.seekTarget(),
                input.deployDecoy(),
                input.mouseX(),
                input.mouseY()
        ));
    }

    @SubscribeEvent
    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        event.setMouseSensitivity(adjustMouseSensitivity(event.getMouseSensitivity()));
    }

    public static double adjustMouseSensitivity(double base) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return base;
        }
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
        return Math.max(0.0F, scaledBase * sensitivity);
    }

    public static boolean handleVehicleMouseTurn(Minecraft minecraft, double accumulatedDX, double accumulatedDY, double frameTime) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return false;
        }
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0 || seatIndex >= vehicle.vehicleData().defaults().seats().size()) {
            return false;
        }

        double sensitivitySetting = minecraft.options.sensitivity().get() * 0.6000000238418579D + 0.20000000298023224D;
        double baseSensitivity = sensitivitySetting * sensitivitySetting * sensitivitySetting * 8.0D;
        double vehicleSensitivity = adjustMouseSensitivity(baseSensitivity);
        double turnX = accumulatedDX * vehicleSensitivity;
        double turnY = accumulatedDY * vehicleSensitivity;
        if (minecraft.options.invertMouseX().get()) {
            turnX = -turnX;
        }
        if (minecraft.options.invertMouseY().get()) {
            turnY = -turnY;
        }
        minecraft.getTutorial().onMouse(turnX, turnY);
        player.turn(turnX, turnY);

        var seat = vehicle.vehicleData().defaults().seats().get(seatIndex);
        clampSeatView(player, vehicle, seat);
        vehicle.refreshClientTurretAim(player);
        return true;
    }

    private static void clampSeatView(LocalPlayer player, VehicleEntity vehicle, ttv.migami.jeg.vehicle.data.subdata.SeatInfo seat) {
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
        float relativeYaw = Mth.wrapDegrees(player.getYRot() - vehicle.getYRot());
        float clampedYaw = Mth.clamp(relativeYaw, seat.minYaw(), seat.maxYaw());
        return vehicle.getYRot() + clampedYaw;
    }

    private static float aircraftBoundPitch(LocalPlayer player, VehicleEntity vehicle, ttv.migami.jeg.vehicle.data.subdata.SeatInfo seat) {
        if (isAircraftDriver(player, vehicle) && seat.sensitivityY() == 0.0F && seat.sensitivityZ() == 0.0F && !VehicleClientState.freeLookDown()) {
            return Mth.clamp(vehicle.getXRot(), seat.minPitch(), seat.maxPitch());
        }
        return Mth.clamp(player.getXRot(), seat.minPitch(), seat.maxPitch());
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

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle) || !(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        if (minecraft.options.keyInventory.matches(event.getKeyEvent())) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            NetworkHandler.sendVehicleOpenMenu(vehicle.getId());
            clearPendingVehicleInventoryClicks();
            event.setCanceled(true);
            return;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(event.getKeyEvent())) {
            VehicleClientState.syncMousePosition(minecraft.mouseHandler.xpos(), minecraft.mouseHandler.ypos());
            minecraft.gui.setScreen(null);
            clearPendingPlayerInventoryClicks();
            event.setCanceled(true);
        }
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
}
