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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(Camera.class)
public abstract class VehicleCameraMixin {
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);

    @Shadow
    protected abstract void move(float x,float y,float z);

    @Shadow
    protected abstract float getMaxZoom(float distance);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

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
        this.jeg$positionVehicleCamera(deltaTracker, callback);
        var shake = ttv.migami.jeg.client.ExplosionShakeHandler.frame();
        if (shake.equals(ttv.migami.jeg.client.ExplosionShakeState.Angles.ZERO)) {
            ttv.migami.jeg.client.ExplosionShakeCheck.frame(shake);
            return;
        }
        // Keep the existing vehicle roll while changing only the rendered camera orientation.
        Quaternionf roll = new Quaternionf().rotationYXZ((float) Math.PI - this.yRot * DEG_TO_RAD,
                -this.xRot * DEG_TO_RAD, 0).conjugate().mul(this.rotation);
        this.setRotation(this.yRot + shake.yaw(), this.xRot + shake.pitch());
        this.rotation.mul(roll).rotateZ(shake.roll() * DEG_TO_RAD);
        this.forwards.set(0, 0, -1).rotate(this.rotation);
        this.up.set(0, 1, 0).rotate(this.rotation);
        this.left.set(1, 0, 0).rotate(this.rotation);
        ttv.migami.jeg.client.ExplosionShakeCheck.frame(shake);
    }

    private void jeg$positionVehicleCamera(DeltaTracker deltaTracker, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity instanceof VehicleEntity fixture && ttv.migami.jeg.vehicle.client.VehicleCaptureCheck.isAiFixture(fixture)) {
            float partialTick = this.getCameraEntityPartialTicks(deltaTracker);
            float yaw = net.minecraft.util.Mth.rotLerp(partialTick, fixture.yRotO, fixture.getYRot());
            Vec3 back = Vec3.directionFromRotation(0, yaw).scale(-16);
            this.setPosition(net.minecraft.util.Mth.lerp(partialTick, fixture.xo, fixture.getX()) + back.x,
                    net.minecraft.util.Mth.lerp(partialTick, fixture.yo, fixture.getY()) + 10,
                    net.minecraft.util.Mth.lerp(partialTick, fixture.zo, fixture.getZ()) + back.z);
            this.jeg$setCameraRotation(new Vec3(yaw, 28, 0));
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || this.entity != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        float partialTick = this.getCameraEntityPartialTicks(deltaTracker);
        CameraType cameraType = minecraft.options.getCameraType();
        boolean detached = !cameraType.isFirstPerson();
        boolean thirdPersonReverse = cameraType.isMirrored();
        if (vehicle.usesSwControls()) {
            ttv.migami.jeg.vehicle.client.VehicleCaptureCheck.freezeFrame(vehicle);
            boolean zoom = ttv.migami.jeg.vehicle.client.VehicleClientState.zoomDown();
            double freeYaw = ttv.migami.jeg.vehicle.client.VehicleClientState.freeYaw();
            double freePitch = ttv.migami.jeg.vehicle.client.VehicleClientState.freePitch();
            double distance = ttv.migami.jeg.vehicle.client.VehicleClientState.cameraDistance();
            if (!detached || zoom) {
                Vec3 position = vehicle.cameraPositionFor(player, partialTick);
                Vec3 rotation = vehicle.cameraRotationFor(player, partialTick);
                if (vehicle.usesAircraftCamera(player)) {
                    rotation = new Vec3(net.minecraft.util.Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot()) - freeYaw,
                            net.minecraft.util.Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot()) + freePitch, detached && !zoom ? 0 : vehicle.swCameraRoll(player, (float) (net.minecraft.util.Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot()) - freeYaw), partialTick));
                }
                this.jeg$setCameraRotation(rotation);
                this.setPosition(position.x, position.y, position.z);
            } else if (vehicle.usesAircraftCamera(player)) {
                Vec3 position = vehicle.swAircraftCameraPosition(player, partialTick, freeYaw, freePitch, distance);
                this.jeg$setCameraRotation(new Vec3(net.minecraft.util.Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot()) - freeYaw,
                        net.minecraft.util.Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot()) + freePitch, 0));
                this.setPosition(position.x, position.y, position.z);
            }
            if (detached && !zoom && !vehicle.usesAircraftCamera(player)) {
                var camera = vehicle.vehicleData().defaults().thirdPersonCamera();
                this.move(-this.getMaxZoom((float) (camera.z() + distance)), (float) camera.y(), (float) camera.x());
            }
            return;
        }
        if (detached) {
            if (!vehicle.usesVehiclePoseTransform()) {
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
        Vec3 cameraPosition = vehicle.cameraPositionFor(player, partialTick);
        Vec3 cameraRotation = vehicle.cameraRotationFor(player, partialTick);
        this.jeg$setCameraRotation(cameraRotation);
        this.setPosition(cameraPosition.x, cameraPosition.y, cameraPosition.z);
    }

    private void jeg$setCameraRotation(Vec3 cameraRotation) {
        float yaw = (float) cameraRotation.x;
        float pitch = (float) cameraRotation.y;
        float roll = (float) cameraRotation.z;
        if (roll == 0.0F) {
            this.setRotation(yaw, pitch);
            return;
        }
        this.setRotation(yaw, pitch); // Invalidate Camera's cached view and projection matrices.
        this.rotation.rotationYXZ((float) Math.PI - yaw * DEG_TO_RAD, -pitch * DEG_TO_RAD, roll * DEG_TO_RAD);
        this.forwards.set(0.0F, 0.0F, -1.0F).rotate(this.rotation);
        this.up.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
        this.left.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
    }
}
