package ttv.migami.jeg.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.vehicle.client.VehicleCameraHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(Camera.class)
public abstract class VehicleCameraMixin implements VehicleCameraHandler.VehicleCameraAccess {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void move(float x, float y, float z);

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    public abstract float getCameraEntityPartialTicks(DeltaTracker deltaTracker);

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    @Final
    private Vector3f forwards;

    @Shadow
    @Final
    private Vector3f up;

    @Shadow
    @Final
    private Vector3f left;

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    private Entity entity;

    @Inject(
            method = "update",
            at = @At("TAIL")
    )
    private void jeg$setupVehicleCamera(DeltaTracker deltaTracker, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || this.entity != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        float partialTick = this.getCameraEntityPartialTicks(deltaTracker);
        CameraType cameraType = minecraft.options.getCameraType();
        boolean detached = !cameraType.isFirstPerson();
        boolean thirdPersonReverse = cameraType.isMirrored();
        if (detached) {
            if (!vehicle.usesVehiclePoseTransform()) {
                VehicleCameraHandler.applyThirdPersonCameraOffset((Camera) (Object) this);
                return;
            }
            Vec3 cameraPosition = vehicle.detachedPoseCameraPositionFor(player, partialTick);
            Vec3 cameraRotation = vehicle.aircraftCameraRotationFor(partialTick);
            float yaw = (float) cameraRotation.x;
            float pitch = (float) cameraRotation.y;
            if (thirdPersonReverse) {
                yaw += 180.0F;
                pitch = -pitch;
                cameraRotation = new Vec3(yaw, pitch, -cameraRotation.z);
            } else {
                cameraRotation = new Vec3(yaw, pitch, cameraRotation.z);
            }
            this.jeg$setCameraRotation(cameraRotation);
            this.setPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z);
            return;
        }
        vehicle.refreshClientTurretAim(player);
        Vec3 cameraPosition = vehicle.cameraPositionFor(player, partialTick);
        Vec3 cameraRotation = vehicle.cameraRotationFor(player, partialTick);
        this.jeg$setCameraRotation(cameraRotation);
        this.setPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z);
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void jeg$adjustVehicleFov(float partialTick, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(VehicleCameraHandler.adjustFov(cir.getReturnValueF()));
    }

    @ModifyVariable(method = "getMaxZoom", at = @At("HEAD"), argsOnly = true)
    private float jeg$adjustVehicleCameraDistance(float startingDistance) {
        return VehicleCameraHandler.thirdPersonCameraDistance(startingDistance);
    }

    @Override
    public void jeg$moveVehicleCamera(float x, float y, float z) {
        this.move(x, y, z);
    }

    private void jeg$setCameraRotation(Vec3 cameraRotation) {
        float yaw = (float) cameraRotation.x;
        float pitch = (float) cameraRotation.y;
        float roll = (float) cameraRotation.z;
        if (roll == 0.0F) {
            this.setRotation(yaw, pitch);
            return;
        }
        this.xRot = pitch;
        this.yRot = yaw;
        this.rotation.rotationYXZ((float) Math.PI - yaw * DEG_TO_RAD, -pitch * DEG_TO_RAD, roll * DEG_TO_RAD);
        this.forwards.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
    }
}
