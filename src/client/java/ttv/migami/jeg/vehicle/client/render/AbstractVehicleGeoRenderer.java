package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.vehicle.client.VehicleClientState;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

abstract class AbstractVehicleGeoRenderer extends GeoEntityRenderer<VehicleEntity> {
    protected AbstractVehicleGeoRenderer(EntityRendererProvider.Context context, GeoModel<VehicleEntity> model) {
        super(context, model);
        this.addRenderLayer(new VehicleBoatWaterMaskLayer(this));
        this.addRenderLayer(new VehicleGlowLayer(this));
        this.shadowRadius = 0.9F;
    }

    @Override
    public RenderType getRenderType(VehicleEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void render(VehicleEntity vehicle, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (shouldHideVehicleWhileZooming(vehicle)) {
            return;
        }
        super.render(vehicle, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void defaultRender(PoseStack poseStack, VehicleEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        if (shouldHideVehicleWhileZooming(animatable)) {
            return;
        }
        poseStack.pushPose();
        this.vehicleAxis(animatable, poseStack, yaw, partialTick);
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(VehicleEntity vehicle, Frustum camera, double camX, double camY, double camZ) {
        if (shouldHideVehicleWhileZooming(vehicle)) {
            return false;
        }
        if (!vehicle.shouldRender(camX, camY, camZ)) {
            return false;
        }
        if (vehicle.noCulling) {
            return true;
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

    private void vehicleAxis(VehicleEntity vehicle, PoseStack poseStack, float entityYaw, float partialTick) {
        float rootY = (float) vehicle.rotateOffsetHeight();
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), 0.0F, rootY, 0.0F);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())), 0.0F, rootY, 0.0F);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(vehicle.roll(partialTick)), 0.0F, rootY, 0.0F);
    }
}
