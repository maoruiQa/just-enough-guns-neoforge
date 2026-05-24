package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.GunMouseSensitivityHandler;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerVehicleTurnMixin {
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

    @WrapOperation(
            method = "turnPlayer(D)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object jeg$adjustVehicleMouseSensitivity(OptionInstance<Double> instance, Operation<Object> original) {
        Object value = original.call(instance);
        if (value instanceof Double sensitivity) {
            return GunMouseSensitivityHandler.adjustRawOptionSensitivity(VehicleInputHandler.adjustMouseSensitivity(sensitivity));
        }
        return value;
    }
}
