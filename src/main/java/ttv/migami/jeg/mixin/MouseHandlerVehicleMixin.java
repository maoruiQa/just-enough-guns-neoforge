package ttv.migami.jeg.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;

@Mixin(MouseHandler.class)
public final class MouseHandlerVehicleMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void jeg$turnVehiclePlayer(double frameTime, CallbackInfo callback) {
        if (VehicleInputHandler.handleVehicleMouseTurn(this.minecraft, this.accumulatedDX, this.accumulatedDY, frameTime)) {
            this.accumulatedDX = 0.0D;
            this.accumulatedDY = 0.0D;
            callback.cancel();
        }
    }
}
