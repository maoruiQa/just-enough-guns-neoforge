package ttv.migami.jeg.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    private static final long DEBUG_WINDOW_NANOS = 2_000_000_000L;
    private static final double THIRD_PERSON_ANIMATED_Y_CORRECTION = -8.75D / 16.0D;
    private static final Set<String> DEBUG_BONES = ConcurrentHashMap.newKeySet();
    private static Method renderModelListsMethod;
    private static volatile long nextRenderByItemDebugNanos;
    private static volatile long nextRenderDebugNanos;
    private static volatile long nextArmDebugNanos;
    private static volatile long nextGuiDebugNanos;

    public AnimatedGunRenderer() {
        super(new AnimatedGunGeoModel());
    }

    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }

    public boolean isFirstPersonContext() {
        ItemDisplayContext ctx = this.renderPerspective;
        if (isFirstPersonDisplay(ctx)) {
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

        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        return ItemStack.isSameItemSameComponents(stack, main) || ItemStack.isSameItemSameComponents(stack, off);
    }

    private static boolean isFirstPersonDisplay(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    public HumanoidArm resolveRenderedHand() {
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

        HumanoidArm mainArm = mc.player.getMainArm();
        ItemStack stack = this.getCurrentItemStack();
        ItemStack main = mc.player.getMainHandItem();
        if (stack != null && ItemStack.isSameItemSameComponents(stack, main)) {
            return mainArm;
        }
        return mainArm == HumanoidArm.LEFT ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (displayContext == ItemDisplayContext.GUI && renderStaticGuiModel(stack, poseStack, bufferSource, packedLight, packedOverlay)) {
            return;
        }

        long now = System.nanoTime();
        if (now >= nextRenderByItemDebugNanos) {
            nextRenderByItemDebugNanos = now + DEBUG_WINDOW_NANOS;
            JustEnoughGuns.LOGGER.info(
                    "[JEG_RENDER_DEBUG] renderByItem item={} perspective={}",
                    stack.getItem(),
                    displayContext
            );
        }
        if (isFirstPersonDisplay(displayContext) && stack.getItem() instanceof AnimatedGunItem gun) {
            poseStack.pushPose();
            try {
                if (renderForgeSpecialModel(stack, gun, poseStack, bufferSource, packedLight, packedOverlay)) {
                    return;
                }
                super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            } finally {
                poseStack.popPose();
            }
            return;
        }
        if (isThirdPersonDisplay(displayContext) && stack.getItem() instanceof AnimatedGunItem gun) {
            poseStack.pushPose();
            try {
                applyThirdPersonAnimatedTransform(gun, poseStack);
                super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            } finally {
                poseStack.popPose();
            }
            return;
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static boolean isThirdPersonDisplay(ItemDisplayContext context) {
        return context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static void applyThirdPersonAnimatedTransform(AnimatedGunItem gun, PoseStack poseStack) {
        if ("minigun".equals(gun.getStats().id().getPath())) {
            // Minigun needs Y correction plus pitch rotation to point forward instead of upward
            poseStack.translate(0.0D, THIRD_PERSON_ANIMATED_Y_CORRECTION - 0.3D, 1.4D);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            return;
        }
        poseStack.translate(0.0D, THIRD_PERSON_ANIMATED_Y_CORRECTION, 0.0D);
    }

    private static boolean renderForgeSpecialModel(
            ItemStack stack,
            AnimatedGunItem gun,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        String itemPath = gun.getStats().id().getPath();
        return switch (itemPath) {
            case "holy_shotgun" -> renderHolyShotgunSpecial(stack, poseStack, bufferSource, packedLight, packedOverlay);
            case "typhoonee" -> renderSpecialModel(Reference.id("special/typhoonee/main"), stack, poseStack, bufferSource, packedLight, packedOverlay);
            default -> false;
        };
    }

    private static boolean renderHolyShotgunSpecial(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        boolean rendered = renderSpecialModel(Reference.id("special/holy_shotgun/main"), stack, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.pushPose();
        try {
            poseStack.translate(0.0D, -5.8D * 0.0625D, 0.0D);
            Minecraft mc = Minecraft.getInstance();
            float cooldown = mc.player == null ? 0.0F : mc.player.getCooldowns().getCooldownPercent(stack.getItem(), mc.getTimer().getGameTimeDeltaPartialTick(false));
            poseStack.translate(0.0D, 0.0D, easeHolyShotgunPump(cooldown) / 14.0D);
            poseStack.translate(0.0D, 5.8D * 0.0625D, 0.0D);
            rendered |= renderSpecialModel(Reference.id("special/holy_shotgun/pumpy"), stack, poseStack, bufferSource, packedLight, packedOverlay);
        } finally {
            poseStack.popPose();
        }

        return rendered;
    }

    private static double easeHolyShotgunPump(double value) {
        return 1.0D - Math.pow(1.0D - 2.0D * value, 4.0D);
    }

    private static boolean renderSpecialModel(
            ResourceLocation modelPath,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        Minecraft mc = Minecraft.getInstance();
        ModelResourceLocation modelId = new ModelResourceLocation(modelPath, "standalone");
        BakedModel model = mc.getModelManager().getModel(modelId);
        if (model == mc.getModelManager().getMissingModel()) {
            return false;
        }

        try {
            Method renderModelLists = renderModelListsMethod();
            RenderType renderType = ItemBlockRenderTypes.getRenderType(stack, true);
            VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, stack.hasFoil());
            renderModelLists.invoke(mc.getItemRenderer(), model, stack, packedLight, packedOverlay, poseStack, buffer);
            return true;
        } catch (ReflectiveOperationException exception) {
            JustEnoughGuns.LOGGER.warn("[JEG_RENDER_DEBUG] special model render failed for {} model={}: {}", stack.getItem(), modelPath, exception.toString());
            return false;
        }
    }

    @Override
    public void renderRecursively(
            PoseStack poseStack,
            AnimatedGunItem animatable,
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
        String boneName = bone.getName();
        ItemStack stack = this.getCurrentItemStack();
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof AnimatedGunItem gun) {
            GunAttachmentVisibility.apply(gun.getStats().id(), bone);
            debugFirstPersonBones(stack, boneName);
        }

        boolean armBone = isArmBone(boneName);
        if (armBone) {
            bone.setHidden(true);
            bone.setChildrenHidden(false);
            renderArmForBone(poseStack, bone, boneName, bufferSource, packedLight);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    private void renderArmForBone(
            PoseStack poseStack,
            GeoBone bone,
            String boneName,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (!isFirstPersonContext()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }

        ItemStack stack = this.getCurrentItemStack();
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof AnimatedGunItem gun)) {
            return;
        }

        GunPoseProfile profile = GunPoseProfile.forGun(gun.getStats().id());
        HumanoidArm activeArm = resolveRenderedHand();
        boolean leftBone = boneName.contains("left");
        if (profile.armMode() == GunPoseProfile.ArmMode.ONE_HANDED) {
            if (leftBone && activeArm != HumanoidArm.LEFT) {
                return;
            }
            if (!leftBone && activeArm != HumanoidArm.RIGHT) {
                return;
            }
        }
        if (leftBone && !profile.renderLeftArm()) {
            return;
        }

        poseStack.pushPose();
        try {
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.translateToPivotPoint(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);

            boolean sleeve = leftBone
                    ? player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE)
                    : player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
            boolean rendered = HandRenderInvoker.renderHand(mc, player, poseStack, bufferSource, packedLight, leftBone ? HumanoidArm.LEFT : HumanoidArm.RIGHT, sleeve, bone);
            debugArmRender(stack, boneName, leftBone ? HumanoidArm.LEFT : HumanoidArm.RIGHT, rendered);
        } finally {
            poseStack.popPose();
        }
    }

    private void debugFirstPersonBones(ItemStack stack, String boneName) {
        if (!isFirstPersonContext()) {
            return;
        }
        if (DEBUG_BONES.size() < 48) {
            DEBUG_BONES.add(boneName);
        }

        long now = System.nanoTime();
        if (now < nextRenderDebugNanos) {
            return;
        }
        nextRenderDebugNanos = now + DEBUG_WINDOW_NANOS;

        Minecraft mc = Minecraft.getInstance();
        boolean sameMain = mc.player != null && ItemStack.isSameItemSameComponents(stack, mc.player.getMainHandItem());
        boolean sameOffhand = mc.player != null && ItemStack.isSameItemSameComponents(stack, mc.player.getOffhandItem());
        JustEnoughGuns.LOGGER.info(
                "[JEG_RENDER_DEBUG] first-person gun renderer active item={} perspective={} hand={} sameMain={} sameOffhand={} sampledBones={}",
                stack.getItem(),
                this.renderPerspective,
                resolveRenderedHand(),
                sameMain,
                sameOffhand,
                DEBUG_BONES
        );
        DEBUG_BONES.clear();
    }

    private static void debugArmRender(ItemStack stack, String boneName, HumanoidArm arm, boolean rendered) {
        long now = System.nanoTime();
        if (now < nextArmDebugNanos) {
            return;
        }
        nextArmDebugNanos = now + DEBUG_WINDOW_NANOS;
        JustEnoughGuns.LOGGER.info(
                "[JEG_RENDER_DEBUG] arm bone render item={} bone={} arm={} rendered={}",
                stack.getItem(),
                boneName,
                arm,
                rendered
        );
    }

    private static boolean renderStaticGuiModel(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Reference.MOD_ID.equals(itemId.getNamespace())) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        ModelResourceLocation modelId = new ModelResourceLocation(Reference.id("item/gui/" + itemId.getPath()), "standalone");
        BakedModel model = mc.getModelManager().getModel(modelId);
        if (model == mc.getModelManager().getMissingModel()) {
            debugGuiModel(stack, modelId, false);
            return false;
        }

        try {
            Method renderModelLists = renderModelListsMethod();
            RenderType renderType = ItemBlockRenderTypes.getRenderType(stack, true);
            VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, true, stack.hasFoil());
            renderModelLists.invoke(mc.getItemRenderer(), model, stack, packedLight, packedOverlay, poseStack, buffer);
            debugGuiModel(stack, modelId, true);
            return true;
        } catch (ReflectiveOperationException exception) {
            JustEnoughGuns.LOGGER.warn("[JEG_RENDER_DEBUG] static GUI model render failed for {}: {}", stack.getItem(), exception.toString());
            return false;
        }
    }

    private static Method renderModelListsMethod() throws NoSuchMethodException {
        Method method = renderModelListsMethod;
        if (method == null) {
            method = ItemRenderer.class.getDeclaredMethod(
                    "renderModelLists",
                    BakedModel.class,
                    ItemStack.class,
                    int.class,
                    int.class,
                    PoseStack.class,
                    VertexConsumer.class
            );
            method.setAccessible(true);
            renderModelListsMethod = method;
        }
        return method;
    }

    private static void debugGuiModel(ItemStack stack, ModelResourceLocation modelId, boolean rendered) {
        long now = System.nanoTime();
        if (now < nextGuiDebugNanos) {
            return;
        }
        nextGuiDebugNanos = now + DEBUG_WINDOW_NANOS;
        JustEnoughGuns.LOGGER.info(
                "[JEG_RENDER_DEBUG] gui static model item={} model={} rendered={}",
                stack.getItem(),
                modelId,
                rendered
        );
    }

    private static boolean isArmBone(String boneName) {
        return "left_arm".equals(boneName) || "right_arm".equals(boneName)
                || "fake_left_arm".equals(boneName) || "fake_right_arm".equals(boneName);
    }
}
