package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.network.VehicleInputPayload;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleInputHandler {
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
        VehicleClientState.update(vehicle, freeLook);
        if (KeyBindings.VEHICLE_CHANGE_SEAT.consumeClick()) {
            NetworkHandler.sendVehicleChangeSeat(vehicle.getId());
        }
        if (KeyBindings.VEHICLE_DISMOUNT.consumeClick()) {
            NetworkHandler.sendVehicleDismount(vehicle.getId());
        }
        if (minecraft.options.keyInventory.consumeClick()) {
            NetworkHandler.sendVehicleOpenMenu(vehicle.getId());
        }
        minecraft.getConnection().send(new VehicleInputPayload(
                vehicle.getId(),
                minecraft.options.keyUp.isDown(),
                minecraft.options.keyDown.isDown(),
                minecraft.options.keyLeft.isDown(),
                minecraft.options.keyRight.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                minecraft.options.keyJump.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                minecraft.options.keyAttack.isDown(),
                freeLook,
                KeyBindings.VEHICLE_SWITCH_WEAPON.consumeClick(),
                KeyBindings.VEHICLE_DEPLOY_DECOY.consumeClick()
        ));
    }
}
