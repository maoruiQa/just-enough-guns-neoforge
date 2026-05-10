package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoRenderer extends GeoEntityRenderer<VehicleEntity> {
    public VehicleGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new VehicleGeoModel());
        this.addRenderLayer(new VehicleBoatWaterMaskLayer(this));
        this.addRenderLayer(new VehicleGlowLayer(this));
        this.shadowRadius = 0.9F;
    }

    @Override
    public RenderType getRenderType(VehicleEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void defaultRender(PoseStack poseStack, VehicleEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();
        this.vehicleAxis(animatable, poseStack, yaw, partialTick);
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        poseStack.popPose();
    }

    private void vehicleAxis(VehicleEntity vehicle, PoseStack poseStack, float entityYaw, float partialTick) {
        float rootY = (float) vehicle.rotateOffsetHeight();
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), 0.0F, rootY, 0.0F);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTick, vehicle.xRotO, vehicle.getXRot())), 0.0F, rootY, 0.0F);
    }
}
