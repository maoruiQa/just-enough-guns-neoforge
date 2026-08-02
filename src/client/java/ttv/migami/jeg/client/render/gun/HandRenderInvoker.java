package ttv.migami.jeg.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import software.bernie.geckolib.cache.object.GeoBone;
import ttv.migami.jeg.JustEnoughGuns;

/**
 * Renders first-person player arms attached to GeckoLib gun bones.
 * Standard JEG bones ({@code left_arm}/{@code right_arm}) use JEG offsets.
 * SW guided-launcher bones ({@code Lefthand}/{@code Righthand}) use SuperbWarfare
 * {@code setupModelFromBone2} orientation so hands are not front/back flipped.
 */
public final class HandRenderInvoker {
    private static final float SCALE_RECIPROCAL = 1.0F / 16.0F;
    private static volatile long nextFailureDebugNanos;

    private HandRenderInvoker() {}

    public static boolean renderHand(
            Minecraft minecraft,
            AbstractClientPlayer player,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            HumanoidArm arm,
            boolean sleeveVisible,
            GeoBone bone
    ) {
        try {
            EntityRenderer<? super AbstractClientPlayer> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
            if (!(renderer instanceof PlayerRenderer playerRenderer)) {
                debugFailure("player renderer was " + renderer);
                return false;
            }

            PlayerModel<AbstractClientPlayer> model = playerRenderer.getModel();
            ModelPart armPart = arm == HumanoidArm.LEFT ? model.leftArm : model.rightArm;
            ModelPart sleevePart = arm == HumanoidArm.LEFT ? model.leftSleeve : model.rightSleeve;
            boolean swHandBone = isSwHandBone(bone);

            poseStack.pushPose();
            try {
                if (swHandBone) {
                    // Match SW AnimationHelper.renderArms micro-offset (1/16 scale space).
                    if (arm == HumanoidArm.LEFT) {
                        poseStack.translate(-SCALE_RECIPROCAL, 2.0F * SCALE_RECIPROCAL, 0.0D);
                    } else {
                        poseStack.translate(SCALE_RECIPROCAL, 2.0F * SCALE_RECIPROCAL, 0.0D);
                    }
                } else {
                    if (arm == HumanoidArm.LEFT) {
                        poseStack.scale(0.67F, 0.8F, 0.67F);
                        poseStack.translate(-0.25D, -0.1D, 0.1625D);
                    } else {
                        poseStack.scale(0.67F, 0.8F, 0.67F);
                        poseStack.translate(0.25D, -0.1D, 0.1625D);
                    }
                }

                VertexConsumer armBuffer = bufferSource.getBuffer(RenderType.entitySolid(player.getSkin().texture()));
                renderPartOverBone(armPart, bone, poseStack, armBuffer, packedLight, arm, swHandBone);
                if (sleeveVisible) {
                    VertexConsumer sleeveBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
                    renderPartOverBone(sleevePart, bone, poseStack, sleeveBuffer, packedLight, arm, swHandBone);
                }
            } finally {
                poseStack.popPose();
            }
            return true;
        } catch (Throwable throwable) {
            debugFailure(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private static boolean isSwHandBone(GeoBone bone) {
        if (bone == null) {
            return false;
        }
        String name = bone.getName();
        return "Lefthand".equals(name) || "Righthand".equals(name);
    }

    private static void debugFailure(String reason) {
        long now = System.nanoTime();
        if (now < nextFailureDebugNanos) {
            return;
        }
        nextFailureDebugNanos = now + 2_000_000_000L;
        JustEnoughGuns.LOGGER.warn("[JEG_RENDER_DEBUG] hand render failed: {}", reason);
    }

    private static void renderPartOverBone(
            ModelPart model,
            GeoBone bone,
            PoseStack poseStack,
            VertexConsumer buffer,
            int packedLight,
            HumanoidArm arm,
            boolean swHandBone
    ) {
        PartState previous = PartState.capture(model);
        try {
            model.visible = true;
            if (swHandBone) {
                // SW setupModelFromBone2 / setupModelFromBone2R — corrects front/back orientation.
                model.setPos(bone.getPivotX(), bone.getPivotY() + 7.0F, bone.getPivotZ());
                if (arm == HumanoidArm.LEFT) {
                    model.xRot = 0.0F;
                    model.yRot = 180.0F * Mth.DEG_TO_RAD;
                    model.zRot = 180.0F * Mth.DEG_TO_RAD;
                } else {
                    model.xRot = 180.0F * Mth.DEG_TO_RAD;
                    model.yRot = 180.0F * Mth.DEG_TO_RAD;
                    model.zRot = 0.0F;
                }
            } else {
                model.setPos(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ());
                model.xRot = 0.0F;
                model.yRot = 0.0F;
                model.zRot = 0.0F;
            }
            model.render(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        } finally {
            previous.restore(model);
        }
    }

    private record PartState(float x, float y, float z, float xRot, float yRot, float zRot, boolean visible) {
        private static PartState capture(ModelPart part) {
            return new PartState(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.visible);
        }

        private void restore(ModelPart part) {
            part.setPos(x, y, z);
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.visible = visible;
        }
    }
}
