package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ttv.migami.jeg.client.GunMouseSensitivityHandler;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerVehicleTurnMixin {
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
