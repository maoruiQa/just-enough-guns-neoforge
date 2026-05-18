package ttv.migami.jeg.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;

@Mixin(MouseHandler.class)
public final class MouseHandlerVehicleMixin {
    @ModifyVariable(method = "turnPlayer", at = @At(value = "STORE"), ordinal = 2)
    private double jeg$adjustVehicleMouseSensitivity(double sensitivity) {
        return VehicleInputHandler.adjustMouseSensitivity(sensitivity);
    }
}
