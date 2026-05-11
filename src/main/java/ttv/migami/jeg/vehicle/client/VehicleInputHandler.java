package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
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
    private static boolean suppressPlayerInventoryClick;

    private VehicleInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            VehicleClientState.clear();
            return;
        }

        boolean freeLook = KeyBindings.VEHICLE_FREE_LOOK.isDown();
        boolean seek = KeyBindings.VEHICLE_SEEK.isDown();
        boolean reload = KeyBindings.RELOAD.consumeClick();
        int weaponSlot = -1;
        for (int index = 0; index < minecraft.options.keyHotbarSlots.length; index++) {
            if (minecraft.options.keyHotbarSlots[index].consumeClick()) {
                weaponSlot = index;
                break;
            }
        }
        VehicleClientState.update(vehicle, freeLook, minecraft.options.keyUse.isDown(), seek);
        if (KeyBindings.VEHICLE_CHANGE_SEAT.consumeClick()) {
            NetworkHandler.sendVehicleChangeSeat(vehicle.getId());
        }
        if (KeyBindings.VEHICLE_DISMOUNT.consumeClick()) {
            vehicle.clearClientControlState();
            VehicleClientState.clear();
            NetworkHandler.sendVehicleDismount(vehicle.getId());
            return;
        }
        if (minecraft.options.keyInventory.consumeClick()) {
            if (minecraft.screen instanceof VehicleScreen) {
                player.closeContainer();
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                NetworkHandler.sendVehicleOpenMenu(vehicle.getId());
            }
        }
        boolean playerInventoryClick = KeyBindings.VEHICLE_PLAYER_INVENTORY.consumeClick();
        if (suppressPlayerInventoryClick) {
            suppressPlayerInventoryClick = false;
            playerInventoryClick = false;
        }
        if (playerInventoryClick) {
            if (minecraft.screen instanceof InventoryScreen) {
                player.closeContainer();
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new InventoryScreen(player));
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
                KeyBindings.VEHICLE_DEPLOY_DECOY.consumeClick()
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
                input.deployDecoy()
        ));
    }

    @SubscribeEvent
    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        int seatIndex = vehicle.getSeatIndex(player);
        if (seatIndex < 0 || seatIndex >= vehicle.vehicleData().defaults().seats().size()) {
            return;
        }
        var seat = vehicle.vehicleData().defaults().seats().get(seatIndex);
        float base = (float) event.getMouseSensitivity();
        float sensitivity = minecraft.options.getCameraType().isFirstPerson() ? seat.sensitivityY() : seat.sensitivityZ();
        if (VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown()) {
            sensitivity = seat.sensitivityX();
        }
        event.setMouseSensitivity(Math.max(0.0F, base * sensitivity));
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity) || !(event.getScreen() instanceof InventoryScreen)) {
            return;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(event.getKeyCode(), event.getScanCode())) {
            player.closeContainer();
            minecraft.setScreen(null);
            suppressPlayerInventoryClick = true;
            event.setCanceled(true);
        }
    }
}
