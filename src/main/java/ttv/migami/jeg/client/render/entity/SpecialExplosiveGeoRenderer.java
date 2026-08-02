package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.item.SpecialExplosiveItem;

/**
 * Orientation rules mirror Superb Warfare C4/Claymore/TM-62 entity renderers.
 * Falls back to item-stack rendering if geo fails for a kind (prevents invisible mines).
 */
public final class SpecialExplosiveGeoRenderer extends GeoEntityRenderer<PlacedExplosiveEntity> {
    public SpecialExplosiveGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new SpecialExplosiveGeoModel());
        this.shadowRadius = 0.2F;
    }

    @Override
    public RenderType getRenderType(PlacedExplosiveEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(getTextureLocation(animatable));
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            PlacedExplosiveEntity entity,
            BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        // SW: C4 uses 0.5 scale; TM62/Claymore use native geo scale (no extra shrink)
        float scale = switch (entity.kind()) {
            case C4 -> 0.5F;
            case CLAYMORE -> 0.5F;
            case TM_62 -> 1.0F;
        };
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void render(PlacedExplosiveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        try {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } catch (Throwable t) {
            // Never leave explosives invisible if geo fails
            renderItemFallback(entity, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    @Override
    public void defaultRender(
            PoseStack poseStack,
            PlacedExplosiveEntity animatable,
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

        switch (animatable.kind()) {
            case CLAYMORE -> poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            case TM_62 -> poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
            case C4 -> {
                if (!animatable.isInGround() && animatable.getDeltaMovement().lengthSqr() > 1.0E-4D) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
                    poseStack.mulPose(Axis.XP.rotationDegrees(xRot + 90.0F));
                } else {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
                    if (Math.abs(xRot) > 1.0F) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
                    }
                }
            }
        }

        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        poseStack.popPose();
    }

    private void renderItemFallback(PlacedExplosiveEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        if (entity.kind() == SpecialExplosiveItem.Kind.TM_62) {
            poseStack.scale(1.25F, 1.25F, 1.25F);
        } else {
            poseStack.scale(0.85F, 0.85F, 0.85F);
        }
        ItemStack stack = entity.pickupStack();
        Minecraft.getInstance().getItemRenderer().renderStatic(
                null, stack, ItemDisplayContext.GROUND, false,
                poseStack, buffers, entity.level(), packedLight, OverlayTexture.NO_OVERLAY, entity.getId()
        );
        poseStack.popPose();
    }
}
