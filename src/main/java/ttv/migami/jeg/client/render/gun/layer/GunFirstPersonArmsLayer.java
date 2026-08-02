package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.GunPoseProfile;
import ttv.migami.jeg.item.AnimatedGunItem;

import java.util.Set;

/**
 * First-person only: renders player-skin arms aligned to GeckoLib arm bones (left_arm/right_arm),
 * so the "two-handed hold" and reload poses come from the gun's animation JSON (1.20.1-style).
 *
 * Third-person: does not render any arms.
 */
public final class GunFirstPersonArmsLayer extends GeoRenderLayer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> {
    private static final Set<String> ARM_BONES = Set.of(
            "left_arm", "right_arm", "fake_left_arm", "fake_right_arm",
            // SW guided-launcher hand anchors (Igla etc.)
            "Lefthand", "Righthand"
    );
    private static final float ARM_WIDTH_SCALE = 0.75F;
    private static final float ARM_HEIGHT_SCALE = 10.0F / 12.0F;
    private static final float ARM_DEPTH_SCALE = 0.75F;
    private static final float LEFT_ARM_Y_OFFSET_PIXELS = -4.133F;
    private static final float RIGHT_ARM_Y_OFFSET_PIXELS = -2.133F;
    private static final float ARM_X_OFFSET_PIXELS = 0.75F;

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

        Item item = passInfo.renderState().getGeckolibData(AnimatedGunRenderer.ANIMATED_ITEM);
        if (!(item instanceof AnimatedGunItem gun)) {
            return;
        }

        String gunId = gun.getStats().id().getPath();
        boolean suppressLeftArm = "typhoonee".equals(gunId);
        GunPoseProfile profile = item instanceof AnimatedGunItem animatedGun
                ? GunPoseProfile.forGun(animatedGun.getStats().id())
                : GunPoseProfile.forGun(Identifier.fromNamespaceAndPath("jeg", "abstract_gun"));
        boolean oneHanded = profile.armMode() == GunPoseProfile.ArmMode.ONE_HANDED;
        boolean renderLeftArm = profile.renderLeftArm();
        ArmSide activeSide = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? ArmSide.LEFT : ArmSide.RIGHT;

        passInfo.addBoneUpdater((info, snapshots) ->
                ARM_BONES.forEach(name -> snapshots.ifPresent(name, snapshot -> {
                    snapshot.skipRender(true);
                    snapshot.skipChildrenRender(false);
                }))
        );

        if (!suppressLeftArm && renderLeftArm && (!oneHanded || activeSide == ArmSide.LEFT)) {
            registerArmBone(passInfo, ArmSide.LEFT);
        }
        if (!oneHanded || activeSide == ArmSide.RIGHT) {
            registerArmBone(passInfo, ArmSide.RIGHT);
        }
    }

    private static void registerArmBone(RenderPassInfo<GeoRenderState> passInfo, ArmSide side) {
        String fake = side == ArmSide.LEFT ? "fake_left_arm" : "fake_right_arm";
        String normal = side == ArmSide.LEFT ? "left_arm" : "right_arm";
        // SW guided launchers (Igla) use Lefthand/Righthand instead of left_arm/right_arm.
        String sw = side == ArmSide.LEFT ? "Lefthand" : "Righthand";
        // Prefer normal arm bones; then SW names; fake_* last (can drift into camera).
        passInfo.model().getBone(normal).ifPresentOrElse(
                bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(side, false)),
                () -> passInfo.model().getBone(sw).ifPresentOrElse(
                        bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(side, true)),
                        () -> passInfo.model().getBone(fake).ifPresent(bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(side, false)))
                )
        );
    }

    private enum ArmSide {
        LEFT, RIGHT
    }

    private static final class ArmRenderTask implements PerBoneRender<GeoRenderState> {
        private final ArmSide side;
        private final boolean swStyle;

        private ArmRenderTask(ArmSide side, boolean swStyle) {
            this.side = side;
            this.swStyle = swStyle;
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
            PlayerModel model = renderer.getModel();
            ModelPart armPart = side == ArmSide.LEFT ? model.leftArm : model.rightArm;
            ModelPart sleevePart = side == ArmSide.LEFT ? model.leftSleeve : model.rightSleeve;
            PlayerModelPart sleeve = side == ArmSide.LEFT ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE;
            boolean sleeveVisible = player.isModelPartShown(sleeve);

            PoseStack poseStack = passInfo.poseStack();
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(skin),
                    (pose, buffer) -> {
                        PoseStack armPose = new PoseStack();
                        armPose.last().set(pose);
                        if (swStyle) {
                            // SW AnimationHelper micro-offset (1/16).
                            float s = 1.0F / 16.0F;
                            if (side == ArmSide.LEFT) {
                                armPose.translate(-s, 2.0F * s, 0.0F);
                            } else {
                                armPose.translate(s, 2.0F * s, 0.0F);
                            }
                        }
                        renderPreparedArmPart(armPart, armPose, buffer, light, side, swStyle);
                        if (sleeveVisible) {
                            renderPreparedArmPart(sleevePart, armPose, buffer, light, side, swStyle);
                        }
                    }
            );
        }

        private static void renderPreparedArmPart(
                ModelPart part,
                PoseStack poseStack,
                com.mojang.blaze3d.vertex.VertexConsumer buffer,
                int light,
                ArmSide side,
                boolean swStyle
        ) {
            PartState state = PartState.capture(part);
            try {
                prepareArmPart(part, side, swStyle);
                part.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
            } finally {
                state.restore(part);
            }
        }

        private static void prepareArmPart(ModelPart part, ArmSide side, boolean swStyle) {
            part.resetPose();
            part.visible = true;
            if (swStyle) {
                // SW setupModelFromBone2 / setupModelFromBone2R — fix front/back flip on Lefthand/Righthand.
                // GeckoLib v5 bone pose is already applied via PerBoneRender pose stack; only fix ModelPart orientation.
                float xOffset = side == ArmSide.LEFT ? -ARM_X_OFFSET_PIXELS : ARM_X_OFFSET_PIXELS;
                float yOffset = side == ArmSide.LEFT ? LEFT_ARM_Y_OFFSET_PIXELS : RIGHT_ARM_Y_OFFSET_PIXELS;
                part.setPos(xOffset, yOffset + 7.0F, 0.0F);
                if (side == ArmSide.LEFT) {
                    part.xRot = 0.0F;
                    part.yRot = (float) Math.PI;
                    part.zRot = (float) Math.PI;
                } else {
                    part.xRot = (float) Math.PI;
                    part.yRot = (float) Math.PI;
                    part.zRot = 0.0F;
                }
                part.xScale = ARM_WIDTH_SCALE;
                part.yScale = ARM_HEIGHT_SCALE;
                part.zScale = ARM_DEPTH_SCALE;
                return;
            }
            float xOffset = side == ArmSide.LEFT ? -ARM_X_OFFSET_PIXELS : ARM_X_OFFSET_PIXELS;
            float yOffset = side == ArmSide.LEFT ? LEFT_ARM_Y_OFFSET_PIXELS : RIGHT_ARM_Y_OFFSET_PIXELS;
            part.setPos(xOffset, yOffset, 0.0F);
            part.xRot = 0.0F;
            part.yRot = 0.0F;
            part.zRot = 0.0F;
            part.xScale = ARM_WIDTH_SCALE;
            part.yScale = ARM_HEIGHT_SCALE;
            part.zScale = ARM_DEPTH_SCALE;
        }

        private record PartState(
                float x,
                float y,
                float z,
                float xRot,
                float yRot,
                float zRot,
                float xScale,
                float yScale,
                float zScale,
                boolean visible
        ) {
            private static PartState capture(ModelPart part) {
                return new PartState(
                        part.x,
                        part.y,
                        part.z,
                        part.xRot,
                        part.yRot,
                        part.zRot,
                        part.xScale,
                        part.yScale,
                        part.zScale,
                        part.visible
                );
            }

            private void restore(ModelPart part) {
                part.setPos(x, y, z);
                part.xRot = xRot;
                part.yRot = yRot;
                part.zRot = zRot;
                part.xScale = xScale;
                part.yScale = yScale;
                part.zScale = zScale;
                part.visible = visible;
            }
        }
    }
}
