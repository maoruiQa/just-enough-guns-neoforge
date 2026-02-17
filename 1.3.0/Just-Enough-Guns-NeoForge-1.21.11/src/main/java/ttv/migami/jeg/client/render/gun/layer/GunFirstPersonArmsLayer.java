package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.PerBoneRender;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtil;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.item.AnimatedGunItem;

/**
 * First-person only: renders player-skin arms aligned to GeckoLib arm bones (left_arm/right_arm),
 * so the "two-handed hold" and reload poses come from the gun's animation JSON (1.20.1-style).
 *
 * Third-person: does not render any arms.
 */
public final class GunFirstPersonArmsLayer extends GeoRenderLayer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> {
    private static final java.util.Set<String> HIDE_ARMS_IN_FIRST_PERSON = java.util.Set.of(
            "double_barrel_shotgun",
            "pump_shotgun",
            "repeating_shotgun",
            "supersonic_shotgun",
            "holy_shotgun",
            "waterpipe_shotgun"
    );

    public GunFirstPersonArmsLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
        ItemDisplayContext ctx = passInfo.renderState()
                .getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);

        if (ctx != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && ctx != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return;
        }

        Item item = passInfo.renderState().getGeckolibData(DataTickets.ITEM);
        String id = item instanceof AnimatedGunItem gun ? gun.getStats().id().getPath() : "abstract_gun";
        if (HIDE_ARMS_IN_FIRST_PERSON.contains(id)) {
            return;
        }

        GunPoseProfile profile = item instanceof AnimatedGunItem gun
                ? GunPoseProfile.forGun(gun.getStats().id())
                : GunPoseProfile.forGun(Identifier.fromNamespaceAndPath("jeg", "abstract_gun"));
        boolean oneHanded = profile.armMode() == GunPoseProfile.ArmMode.ONE_HANDED;
        boolean renderLeftArm = profile.renderLeftArm();
        ArmSide activeSide = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? ArmSide.LEFT : ArmSide.RIGHT;

        if (renderLeftArm && (!oneHanded || activeSide == ArmSide.LEFT)) {
            registerArmBone(passInfo, ArmSide.LEFT, profile.leftArm());
        }
        if (!oneHanded || activeSide == ArmSide.RIGHT) {
            registerArmBone(passInfo, ArmSide.RIGHT, profile.rightArm());
        }
    }

    private static void registerArmBone(RenderPassInfo<GeoRenderState> passInfo, ArmSide side, GunPoseProfile.ArmTransform transform) {
        String fake = side == ArmSide.LEFT ? "fake_left_arm" : "fake_right_arm";
        String normal = side == ArmSide.LEFT ? "left_arm" : "right_arm";
        // Prefer normal arm bones; fake_* are often emergency anchors that can drift into camera.
        passInfo.model().getBone(normal).ifPresentOrElse(
                bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(side, transform)),
                () -> passInfo.model().getBone(fake).ifPresent(bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(side, transform)))
        );
    }

    private enum ArmSide {
        LEFT, RIGHT
    }

    private static final class ArmRenderTask implements PerBoneRender<GeoRenderState> {
        private final ArmSide side;
        private final GunPoseProfile.ArmTransform transform;

        private ArmRenderTask(ArmSide side, GunPoseProfile.ArmTransform transform) {
            this.side = side;
            this.transform = transform;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<GeoRenderState> passInfo, GeoBone bone, SubmitNodeCollector collector) {
            AbstractClientPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isInvisible()) {
                return;
            }

            AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
            Identifier skin = player.getSkin().body().texturePath();
            int light = passInfo.packedLight();

            PoseStack poseStack = passInfo.poseStack();
            poseStack.pushPose();

            // Align to the animated arm bone.
            RenderUtil.prepMatrixForBone(poseStack, bone);

            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(transform.rx()));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(transform.ry()));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(transform.rz()));
            poseStack.scale(transform.sx(), transform.sy(), transform.sz());
            if (side == ArmSide.LEFT) {
                poseStack.translate(transform.tx(), transform.ty(), transform.tz());
                renderer.renderLeftHand(
                        poseStack,
                        collector,
                        light,
                        skin,
                        player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE),
                        player
                );
            } else {
                poseStack.translate(transform.tx(), transform.ty(), transform.tz());
                renderer.renderRightHand(
                        poseStack,
                        collector,
                        light,
                        skin,
                        player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE),
                        player
                );
            }

            poseStack.popPose();
        }
    }
}
