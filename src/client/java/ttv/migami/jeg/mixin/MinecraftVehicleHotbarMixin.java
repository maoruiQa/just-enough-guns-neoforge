package ttv.migami.jeg.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;

@Mixin(Minecraft.class)
public class MinecraftVehicleHotbarMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    @Final
    public Options options;

    @Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
    private void jeg$handleMountedInventoryKeys(CallbackInfo callback) {
        if (this.player == null || !(this.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (VehicleInputHandler.shouldIgnoreVehicleInventoryKey() && this.options.keyInventory.consumeClick()) {
            callback.cancel();
            return;
        }
        if (VehicleInputHandler.shouldIgnorePlayerInventoryKey() && KeyBindings.VEHICLE_PLAYER_INVENTORY.consumeClick()) {
            callback.cancel();
            return;
        }
        if (!VehicleInputHandler.shouldIgnoreVehicleInventoryKey() && this.options.keyInventory.consumeClick()) {
            VehicleInputHandler.syncMouseToCurrentCursor();
            ClientNetworkHandler.sendVehicleOpenMenu(vehicle.getId());
            VehicleInputHandler.clearPendingVehicleInventoryClicks();
            callback.cancel();
            return;
        }
        if (!VehicleInputHandler.shouldIgnorePlayerInventoryKey() && KeyBindings.VEHICLE_PLAYER_INVENTORY.consumeClick()) {
            VehicleInputHandler.syncMouseToCurrentCursor();
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(this.player));
            }
            VehicleInputHandler.clearPendingPlayerInventoryClicks();
            callback.cancel();
        }
    }

    @Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
    private void jeg$handleVehicleHotbarKeys(CallbackInfo callback) {
        if (this.player == null || !(this.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        int weaponSlot = -1;
        for (int index = 0; index < this.options.keyHotbarSlots.length; index++) {
            if (this.options.keyHotbarSlots[index].isDown()) {
                weaponSlot = index;
                break;
            }
        }
        if (weaponSlot < 0) {
            return;
        }
        callback.cancel();
        this.options.keyHotbarSlots[weaponSlot].consumeClick();
        int vehicleWeaponSlot = vehicle.vehicleWeaponIndexForDisplaySlot(this.player, weaponSlot);
        if (vehicleWeaponSlot < 0) {
            return;
        }
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        VehicleInput input = new VehicleInput(
                this.options.keyUp.isDown(),
                this.options.keyDown.isDown(),
                this.options.keyLeft.isDown(),
                this.options.keyRight.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                this.options.keyJump.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                this.options.keyAttack.isDown(),
                false,
                KeyBindings.VEHICLE_FREE_LOOK.isDown(),
                false,
                false,
                vehicleWeaponSlot,
                KeyBindings.VEHICLE_SEEK.isDown(),
                false,
                0.0F,
                0.0F
        );
        vehicle.processClientInput(this.player, input);
        ClientNetworkHandler.sendVehicleInput(vehicle.getId(), input);
    }
}
