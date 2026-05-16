package ttv.migami.jeg.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.item.RepairToolItem;

public final class RepairToolItemRenderer extends GeoItemRenderer<RepairToolItem> {
    private static final String RIGHT_HAND_BONE = "Righthand";
    private static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("jeg:repair_tool_item_stack", ItemStack.class);

    public RepairToolItemRenderer() {
        super(new RepairToolItemModel());
        this.withRenderLayer(new RepairToolArmLayer(this));
    }

    @Override
    public GeoRenderState createRenderState(RepairToolItem animatable, GeoItemRenderer.RenderData context) {
        GeoRenderState state = super.createRenderState(animatable, context);
        captureItemRenderContext(context, state);
        return state;
    }

    @Override
    public void captureDefaultRenderState(RepairToolItem animatable, GeoItemRenderer.RenderData context, GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);
        captureItemRenderContext(context, renderState);
    }

    @Override
    public RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    private static void captureItemRenderContext(GeoItemRenderer.RenderData context, GeoRenderState renderState) {
        ItemDisplayContext perspective = resolveStableContext(
                context != null ? context.renderPerspective() : ItemDisplayContext.NONE,
                context != null ? context.itemStack() : ItemStack.EMPTY
        );
        renderState.addGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        if (context != null) {
            renderState.addGeckolibData(ITEM_STACK, context.itemStack());
        }
    }

    private static ItemDisplayContext resolveStableContext(GeoRenderState renderState) {
        ItemDisplayContext base = renderState.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        ItemStack stack = renderState.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        return resolveStableContext(base, stack);
    }

    private static ItemDisplayContext resolveStableContext(ItemDisplayContext base, ItemStack stack) {
        if (base != null && base != ItemDisplayContext.NONE) {
            return base;
        }
        if (stack == null || stack.isEmpty()) {
            return ItemDisplayContext.NONE;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return ItemDisplayContext.NONE;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        if (matchesHeldStack(stack, mainHand)) {
            return mainHandContext(mc.player.getMainArm());
        }
        if (matchesHeldStack(stack, offHand)) {
            return offHandContext(mc.player.getMainArm());
        }

        return ItemDisplayContext.NONE;
    }

    private static boolean matchesHeldStack(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == heldStack) {
            return true;
        }
        if (renderStack == null || renderStack.isEmpty() || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(renderStack, heldStack)
                || ItemStack.isSameItem(renderStack, heldStack);
    }

    private static ItemDisplayContext mainHandContext(HumanoidArm mainArm) {
        return mainArm == HumanoidArm.LEFT
                ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static ItemDisplayContext offHandContext(HumanoidArm mainArm) {
        return mainArm == HumanoidArm.LEFT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    private static boolean isFirstPerson(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static HumanoidArm renderedArm(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    private static final class RepairToolArmLayer extends GeoRenderLayer<RepairToolItem, GeoItemRenderer.RenderData, GeoRenderState> {
        private static final float ARM_WIDTH_SCALE = 0.75F;
        private static final float ARM_HEIGHT_SCALE = 10.0F / 12.0F;
        private static final float ARM_DEPTH_SCALE = 0.75F;
        private static final float LEFT_ARM_Y_OFFSET_PIXELS = -4.133F;
        private static final float RIGHT_ARM_Y_OFFSET_PIXELS = -2.133F;
        private static final float ARM_X_OFFSET_PIXELS = 0.75F;

        private RepairToolArmLayer(GeoItemRenderer<RepairToolItem> renderer) {
            super(renderer);
        }

        @Override
        public void preRender(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
            ItemDisplayContext ctx = resolveStableContext(passInfo.renderState());
            if (!isFirstPerson(ctx)) {
                return;
            }

            passInfo.addBoneUpdater((info, snapshots) ->
                    snapshots.ifPresent(RIGHT_HAND_BONE, snapshot -> {
                        snapshot.skipRender(true);
                        snapshot.skipChildrenRender(false);
                    })
            );
            passInfo.model().getBone(RIGHT_HAND_BONE)
                    .ifPresent(bone -> passInfo.addPerBoneRender(bone, new ArmRenderTask(renderedArm(ctx))));
        }

        private enum ArmSide {
            LEFT,
            RIGHT
        }

        private static final class ArmRenderTask implements PerBoneRender<GeoRenderState> {
            private final ArmSide side;

            private ArmRenderTask(HumanoidArm arm) {
                this.side = arm == HumanoidArm.LEFT ? ArmSide.LEFT : ArmSide.RIGHT;
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
                            renderPreparedArmPart(armPart, armPose, buffer, light, side);
                            if (sleeveVisible) {
                                renderPreparedArmPart(sleevePart, armPose, buffer, light, side);
                            }
                        }
                );
            }

            private static void renderPreparedArmPart(
                    ModelPart part,
                    PoseStack poseStack,
                    com.mojang.blaze3d.vertex.VertexConsumer buffer,
                    int light,
                    ArmSide side
            ) {
                PartState state = PartState.capture(part);
                try {
                    prepareArmPart(part, side);
                    part.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
                } finally {
                    state.restore(part);
                }
            }

            private static void prepareArmPart(ModelPart part, ArmSide side) {
                part.resetPose();
                part.visible = true;
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
}
