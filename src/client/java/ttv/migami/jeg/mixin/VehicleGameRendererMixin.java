package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.GunCameraSwayHandler;
import ttv.migami.jeg.client.util.ScreenProjection;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(GameRenderer.class)
public abstract class VehicleGameRendererMixin {
    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void jeg$applyVehicleCameraView(
            DeltaTracker deltaTracker,
            CallbackInfo callback,
            @Local CameraRenderState cameraRenderState,
            @Local PoseStack poseStack
    ) {
        float partialTick = this.mainCamera.getCameraEntityPartialTicks(deltaTracker);
        GunCameraSwayHandler.apply(poseStack, partialTick);

        // Capture world→screen matrices for guided-launcher / drone seek frames.
        // Without this, ScreenProjection.worldToScreen uses identity matrices and frames never land on entities.
        try {
            Matrix4f modelView = new Matrix4f(poseStack.last().pose());
            Matrix4f projection = cameraRenderState.projectionMatrix != null
                    ? new Matrix4f(cameraRenderState.projectionMatrix)
                    : new Matrix4f();
            ScreenProjection.captureMatrices(modelView, projection);
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) {
                double fov = cameraRenderState.hudFov > 0.0F
                        ? cameraRenderState.hudFov
                        : mc.options.fov().get();
                ScreenProjection.setFov(fov);
            }
        } catch (Throwable ignored) {
        }

        Entity entity = this.mainCamera.entity();
        if (entity == null || !(entity.getRootVehicle() instanceof VehicleEntity vehicle) || this.mainCamera.isDetached()) {
            return;
        }
        boolean fixedCamera = vehicle.usesFixedCameraPosition(entity);
        float yawDelta = Mth.wrapDegrees(this.mainCamera.yRot() - Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot()));
        float rollFactor = (Mth.abs(yawDelta) - 90.0F) / 90.0F;
        float pitchFactor;
        if (Mth.abs(yawDelta) <= 90.0F) {
            pitchFactor = yawDelta / 90.0F;
        } else if (yawDelta < 0.0F) {
            pitchFactor = -(180.0F + yawDelta) / 90.0F;
        } else {
            pitchFactor = (180.0F - yawDelta) / 90.0F;
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(-rollFactor * vehicle.roll(partialTick) - pitchFactor * Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())));

        if (fixedCamera) {
            return;
        }

        float eyeHeight = entity.getEyeHeight();
        Vector3f offset = new Vector3f(0.0F, -eyeHeight, 0.0F);
        Quaternionf quaternion = Axis.XP.rotationDegrees(0.0F);
        quaternion.mul(Axis.YP.rotationDegrees(-Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot())));
        quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())));
        quaternion.mul(Axis.ZP.rotationDegrees(vehicle.roll(partialTick)));
        offset.rotate(quaternion);

        poseStack.mulPose(Axis.XP.rotationDegrees(this.mainCamera.xRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(this.mainCamera.yRot() + 180.0F));
        poseStack.translate(offset.x(), offset.y() + eyeHeight, offset.z());
        poseStack.mulPose(Axis.YP.rotationDegrees(-this.mainCamera.yRot() - 180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-this.mainCamera.xRot()));
    }
}
