package ttv.migami.jeg.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.client.render.gun.HandRenderInvoker;
import ttv.migami.jeg.item.RepairToolItem;

public final class RepairToolItemRenderer extends GeoItemRenderer<RepairToolItem> {
    private static final String RIGHT_HAND_BONE = "Righthand";

    public RepairToolItemRenderer() {
        super(new RepairToolItemModel());
    }

    @Override
    public RenderType getRenderType(RepairToolItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            RepairToolItem animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int color
    ) {
        if (RIGHT_HAND_BONE.equals(bone.getName())) {
            bone.setHidden(true);
            bone.setChildrenHidden(false);
            if (isFirstPersonContext()) {
                renderPlayerArmForBone(poseStack, bone, bufferSource, packedLight);
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    private void renderPlayerArmForBone(
            PoseStack poseStack,
            GeoBone bone,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        HumanoidArm arm = resolveRenderedArm();
        boolean sleeve = arm == HumanoidArm.LEFT
                ? player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE)
                : player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
        HandRenderInvoker.renderHand(mc, player, poseStack, bufferSource, packedLight, arm, sleeve, bone);
    }

    private boolean isFirstPersonContext() {
        ItemDisplayContext ctx = this.renderPerspective;
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return true;
        }
        if (ctx != null && ctx != ItemDisplayContext.NONE) {
            return false;
        }

        ItemStack stack = this.getCurrentItemStack();
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !mc.options.getCameraType().isFirstPerson()) {
            return false;
        }

        return isSameHeldItem(stack, mc.player.getMainHandItem()) || isSameHeldItem(stack, mc.player.getOffhandItem());
    }

    private HumanoidArm resolveRenderedArm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return HumanoidArm.RIGHT;
        }

        ItemDisplayContext ctx = this.renderPerspective;
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return HumanoidArm.LEFT;
        }
        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return HumanoidArm.RIGHT;
        }

        ItemStack stack = this.getCurrentItemStack();
        HumanoidArm mainArm = mc.player.getMainArm();
        if (isSameHeldItem(stack, mc.player.getMainHandItem())) {
            return mainArm;
        }
        if (isSameHeldItem(stack, mc.player.getOffhandItem())) {
            return mainArm == HumanoidArm.LEFT ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        }
        return mainArm;
    }

    private static boolean isSameHeldItem(ItemStack renderStack, ItemStack heldStack) {
        if (renderStack == heldStack) {
            return true;
        }
        if (renderStack == null || renderStack.isEmpty() || heldStack == null || heldStack.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(renderStack, heldStack)
                || ItemStack.isSameItem(renderStack, heldStack);
    }
}
