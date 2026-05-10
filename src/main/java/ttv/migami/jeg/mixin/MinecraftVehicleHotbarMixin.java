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
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.network.VehicleInputPayload;

@Mixin(Minecraft.class)
public class MinecraftVehicleHotbarMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    @Final
    public Options options;

    @Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
    private void jeg$handleVehicleHotbarKeys(CallbackInfo callback) {
        if (this.player == null || !(this.player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (this.options.keyInventory.isDown()) {
            callback.cancel();
            this.options.keyInventory.consumeClick();
            NetworkHandler.sendVehicleOpenMenu(vehicle.getId());
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
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        Minecraft.getInstance().getConnection().send(new VehicleInputPayload(
                vehicle.getId(),
                this.options.keyUp.isDown(),
                this.options.keyDown.isDown(),
                this.options.keyLeft.isDown(),
                this.options.keyRight.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                this.options.keyJump.isDown(),
                KeyBindings.VEHICLE_BRAKE_DESCEND.isDown(),
                this.options.keyAttack.isDown(),
                KeyBindings.VEHICLE_FREE_LOOK.isDown(),
                false,
                false,
                weaponSlot,
                KeyBindings.VEHICLE_SEEK.isDown(),
                false
        ));
    }
}
