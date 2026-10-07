package ttv.migami.jeg.mixin;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class VehicleHitboxRenderMixin {
    @Inject(method = "showHitboxes", at = @At("RETURN"))
    private void jeg$vehicleParts(Entity entity, float partialTick, boolean serverEntity, CallbackInfo ci) {
        if (!(entity instanceof VehicleEntity vehicle) || OBBInfo.DEFAULT.equals(vehicle.vehicleData().defaults().obb())) return;
        for (var box : VehicleGeometry.boxes(vehicle, partialTick)) {
            int color = box.part() == OBBInfo.Part.INTERACTIVE ? 0xFFFFCC00 : 0xFF00FF00;
            for (int corner = 0; corner < 8; corner++) for (int axis = 1; axis <= 4; axis <<= 1) {
                if ((corner & axis) == 0) Gizmos.line(box.corner(corner), box.corner(corner | axis), color);
            }
        }
    }
}
