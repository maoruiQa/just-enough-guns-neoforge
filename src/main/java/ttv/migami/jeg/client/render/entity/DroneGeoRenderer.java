package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.entity.DroneEntity;

/**
 * 3D geo drone — Superb Warfare style yaw/pitch body orientation + spinning rotors.
 */
public final class DroneGeoRenderer extends GeoEntityRenderer<DroneEntity> {
    public DroneGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneGeoModel());
        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(DroneEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            DroneEntity animatable,
            BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        // Spin propellers if present (SW wingFL/FR/BL/BR)
        float spin = (System.currentTimeMillis() % 36000000L) / 12.0F;
        for (String name : new String[]{"wingFL", "wingFR", "wingBL", "wingBR", "propeller", "prop"}) {
            try {
                GeoBone bone = model.getBone(name).orElse(null);
                if (bone != null) {
                    bone.setRotY(spin);
                }
            } catch (Exception ignored) {
            }
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void defaultRender(
            PoseStack poseStack,
            DroneEntity animatable,
            MultiBufferSource bufferSource,
            @Nullable RenderType renderType,
            @Nullable VertexConsumer buffer,
            float yaw,
            float partialTick,
            int packedLight
    ) {
        poseStack.pushPose();
        float yRot = Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot());
        float xRot = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        poseStack.popPose();
    }
}
