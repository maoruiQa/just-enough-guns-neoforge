package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

public final class VehicleMissileRenderer extends EntityRenderer<VehicleMissileEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/end_rod.png");

    public VehicleMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(VehicleMissileEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 motion = entity.getDeltaMovement();
        poseStack.pushPose();
        if (motion.lengthSqr() > 1.0E-4D) {
            double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG)));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) (Mth.atan2(motion.y, horizontal) * -Mth.RAD_TO_DEG)));
        }
        poseStack.scale(0.18F, 0.18F, 0.65F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.END_ROD.defaultBlockState(),
                poseStack,
                bufferSource,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VehicleMissileEntity entity) {
        return TEXTURE;
    }
}
