package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.client.render.gun.HandRenderInvoker;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class GunFirstPersonArmsLayer extends GeoRenderLayer<AnimatedGunItem> {
    private static volatile boolean operational = true;
    private static final long RECENT_ARM_RENDER_WINDOW_NANOS = 50_000_000L;
    private static volatile long lastArmRenderNanos = Long.MIN_VALUE;

    public GunFirstPersonArmsLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    public static boolean isOperational() {
        return operational;
    }

    public static boolean wasArmRenderedRecently() {
        long last = lastArmRenderNanos;
        return last != Long.MIN_VALUE && System.nanoTime() - last <= RECENT_ARM_RENDER_WINDOW_NANOS;
    }

    @Override
    public void renderForBone(
            PoseStack poseStack,
            AnimatedGunItem animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!operational) {
            return;
        }

        if (!(getRenderer() instanceof AnimatedGunRenderer renderer)) {
            return;
        }
        if (!renderer.isFirstPersonContext()) {
            return;
        }

        String boneName = bone.getName();
        if (!"left_arm".equals(boneName) && !"right_arm".equals(boneName)
                && !"fake_left_arm".equals(boneName) && !"fake_right_arm".equals(boneName)) {
            return;
        }
        bone.setHidden(true);
        bone.setChildrenHidden(false);

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        ItemStack stack = renderer.getCurrentItemStack();
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem gun)) {
            return;
        }

        GunPoseProfile profile = GunPoseProfile.forGun(gun.getStats().id());
        HumanoidArm activeArm = renderer.resolveRenderedHand();
        boolean isLeftBone = boneName.contains("left");
        if (profile.armMode() == GunPoseProfile.ArmMode.ONE_HANDED) {
            if (isLeftBone && activeArm != HumanoidArm.LEFT) {
                return;
            }
            if (!isLeftBone && activeArm != HumanoidArm.RIGHT) {
                return;
            }
        }
        if (isLeftBone && !profile.renderLeftArm()) {
            return;
        }

        GunPoseProfile.ArmTransform t = isLeftBone ? profile.leftArm() : profile.rightArm();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(t.rx()));
        poseStack.mulPose(Axis.YP.rotationDegrees(t.ry()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(t.rz()));
        poseStack.scale(t.sx(), t.sy(), t.sz());
        poseStack.translate(t.tx(), t.ty(), t.tz());

        boolean sleeve = isLeftBone
                ? player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE)
                : player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        HumanoidArm arm = isLeftBone ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
        if (HandRenderInvoker.renderHand(mc, player, poseStack, bufferSource, packedLight, arm, sleeve, bone)) {
            lastArmRenderNanos = System.nanoTime();
        } else {
            // Keep layer alive; event fallback handles frames where bone hand rendering fails.
            operational = true;
        }
        poseStack.popPose();
    }

    @Override
    public void render(
            PoseStack poseStack,
            AnimatedGunItem animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            com.mojang.blaze3d.vertex.VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        // Per-bone only.
    }
}
