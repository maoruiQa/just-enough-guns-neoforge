package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

abstract class AbstractVehicleGeoRenderer<T extends VehicleEntity> extends GeoEntityRenderer<T, VehicleRenderState> {
    @SuppressWarnings("unchecked")
    protected AbstractVehicleGeoRenderer(EntityRendererProvider.Context context, GeoModel<? extends VehicleEntity> model) {
        super(context, (GeoModel<T>) model);
        this.withRenderLayer(new VehicleBoatWaterMaskLayer<>(this));
        this.withRenderLayer(new VehicleGlowLayer<>(this));
        this.shadowRadius = 0.9F;
    }

    @Override
    public RenderType getRenderType(VehicleRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public VehicleRenderState createRenderState(T entity, Void context) {
        return new VehicleRenderState();
    }

    @Override
    public void extractRenderState(T vehicle, VehicleRenderState renderState, float partialTick) {
        super.extractRenderState(vehicle, renderState, partialTick);
        renderState.vehicle = vehicle;
        renderState.hideWhileZooming = shouldHideVehicleWhileZooming(vehicle);
        renderState.rootY = (float) vehicle.rotateOffsetHeight();
        renderState.yaw = Mth.rotLerp(partialTick, vehicle.yRotO, vehicle.getYRot());
        renderState.pitch = Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot());
        renderState.roll = vehicle.roll(partialTick);
    }

    @Override
    public void submit(VehicleRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (renderState.hideWhileZooming) {
            return;
        }
        super.submit(renderState, poseStack, collector, cameraState);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<VehicleRenderState> passInfo) {
        if (passInfo.renderState().hideWhileZooming) {
            return;
        }
        this.vehicleAxis(passInfo.renderState(), passInfo.poseStack());
    }

    @Override
    public void preRenderPass(RenderPassInfo<VehicleRenderState> passInfo, SubmitNodeCollector collector) {
        VehicleEntity vehicle = passInfo.renderState().vehicle;
        if (vehicle != null) {
            passInfo.addBoneUpdater((info, snapshots) -> updateVehicleBones(vehicle, info.renderState().ageInTicks, snapshots));
        }
        super.preRenderPass(passInfo, collector);
    }

    @Override
    public boolean shouldRender(T vehicle, Frustum camera, double camX, double camY, double camZ) {
        if (shouldHideVehicleWhileZooming(vehicle)) {
            return false;
        }
        if (!vehicle.shouldRender(camX, camY, camZ)) {
            return false;
        }
        AABB bounds = vehicle.getBoundingBox().inflate(5.0D);
        if (bounds.hasNaN() || bounds.getSize() == 0.0D) {
            bounds = new AABB(vehicle.getX() - 8.0D, vehicle.getY() - 6.0D, vehicle.getZ() - 8.0D, vehicle.getX() + 8.0D, vehicle.getY() + 6.0D, vehicle.getZ() + 8.0D);
        }
        return camera.isVisible(bounds);
    }

    private static boolean shouldHideVehicleWhileZooming(VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getVehicle() == vehicle
                && vehicle.hasFocusedSightHud(player)
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }

    private void vehicleAxis(VehicleRenderState renderState, PoseStack poseStack) {
        float rootY = renderState.rootY;
        poseStack.rotateAround(Axis.YP.rotationDegrees(180.0F - renderState.yaw), 0.0F, rootY, 0.0F);
        poseStack.rotateAround(Axis.XP.rotationDegrees(-renderState.pitch), 0.0F, rootY, 0.0F);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(-renderState.roll), 0.0F, rootY, 0.0F);
    }

    private static void updateVehicleBones(VehicleEntity vehicle, float ageInTicks, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        float partialTick = ageInTicks - vehicle.tickCount;
        switch (vehicle.vehicleDataId().getPath()) {
            case "ah6" -> updateHelicopterRotors(vehicle, partialTick, snapshots);
            case "bmp2" -> updateTurret(vehicle, partialTick, snapshots, -74.0F, 7.5F);
            case "lav150" -> updateTurret(vehicle, partialTick, snapshots, -15.0F, 32.5F);
            case "mi28" -> {
                updateTurret(vehicle, partialTick, snapshots, -40.0F, 10.0F);
                updateHelicopterRotors(vehicle, partialTick, snapshots);
            }
            case "speedboat" -> updateTurret(vehicle, partialTick, snapshots, -25.0F, 50.0F);
            default -> {
            }
        }
    }

    private static void updateTurret(VehicleEntity vehicle, float partialTick, com.geckolib.renderer.base.BoneSnapshots snapshots, float minPitch, float maxPitch) {
        snapshots.ifPresent("turret", snapshot -> snapshot.setRotY(vehicle.turretYaw(partialTick) * Mth.DEG_TO_RAD));
        snapshots.ifPresent("barrel", snapshot -> snapshot.setRotX(Mth.clamp(-vehicle.turretPitch(partialTick), minPitch, maxPitch) * Mth.DEG_TO_RAD));
    }

    private static void updateHelicopterRotors(VehicleEntity vehicle, float partialTick, com.geckolib.renderer.base.BoneSnapshots snapshots) {
        float rotorRotation = vehicle.propellerRot(partialTick);
        snapshots.ifPresent("propeller", snapshot -> snapshot.setRotY(rotorRotation));
        snapshots.ifPresent("tailPropeller", snapshot -> snapshot.setRotX(-6.0F * rotorRotation));
    }
}
