package ttv.migami.jeg.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(Camera.class)
public abstract class VehicleCameraMixin {
    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0),
            cancellable = true
    )
    private void jeg$setupVehicleCamera(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo callback) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || detached || entity != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        Vec3 cameraPosition = vehicle.cameraPositionFor(player, partialTick);
        Vec3 cameraRotation = vehicle.cameraRotationFor(player, partialTick);
        this.setRotation((float) cameraRotation.x, (float) cameraRotation.y);
        this.setPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z);
        callback.cancel();
    }
}
