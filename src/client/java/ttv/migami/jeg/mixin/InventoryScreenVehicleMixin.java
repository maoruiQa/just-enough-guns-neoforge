package ttv.migami.jeg.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(AbstractContainerScreen.class)
public final class InventoryScreenVehicleMixin {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void jeg$handleVehicleInventoryKeys(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof InventoryScreen)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (minecraft.options.keyInventory.matches(event)) {
            VehicleInputHandler.syncMouseToCurrentCursor();
            ClientNetworkHandler.sendVehicleOpenMenu(vehicle.getId());
            VehicleInputHandler.clearPendingVehicleInventoryClicks();
            callback.setReturnValue(true);
            return;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(event)) {
            VehicleInputHandler.syncMouseToCurrentCursor();
            minecraft.setScreenAndShow(null);
            VehicleInputHandler.clearPendingPlayerInventoryClicks();
            callback.setReturnValue(true);
        }
    }
}
