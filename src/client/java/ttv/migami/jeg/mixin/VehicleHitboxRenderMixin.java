package ttv.migami.jeg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;

@Mixin(EntityRenderDispatcher.class)
public abstract class VehicleHitboxRenderMixin {
    @Inject(method = "renderHitbox", at = @At("RETURN"))
    private static void jeg$vehicleParts(PoseStack poseStack, VertexConsumer buffer, Entity entity,
                                        float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!(entity instanceof VehicleEntity vehicle) || OBBInfo.DEFAULT.equals(vehicle.vehicleData().defaults().obb())) return;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 origin = vehicle.getPosition(partialTick);
        for (var box : VehicleGeometry.boxes(vehicle, partialTick)) {
            int color = box.part() == OBBInfo.Part.INTERACTIVE ? 0xFFFFCC00 : 0xFF00FF00;
            for (int corner = 0; corner < 8; corner++) for (int axis = 1; axis <= 4; axis <<= 1) {
                if ((corner & axis) != 0) continue;
                Vec3 from = box.corner(corner).subtract(origin), to = box.corner(corner | axis).subtract(origin);
                Vec3 normal = to.subtract(from).normalize();
                for (Vec3 point : new Vec3[]{from, to}) buffer.addVertex(poseStack.last(), (float) point.x, (float) point.y, (float) point.z)
                        .setColor(color).setNormal(poseStack.last(), (float) normal.x, (float) normal.y, (float) normal.z);
            }
        }
    }
}
