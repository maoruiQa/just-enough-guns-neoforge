package ttv.migami.jeg.client.render.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.RenderUtil;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.GunClientEvents;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.layer.GunBayonetAttachmentLayer;
import ttv.migami.jeg.client.render.gun.layer.GunPositionedAttachmentLayer;
import ttv.migami.jeg.client.render.gun.layer.GunScopeAttachmentLayer;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.GunAttachments;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    private static final long DEBUG_WINDOW_NANOS = 2_000_000_000L;
    private static final double FIRST_PERSON_TRANSLATE_X = -3.0D / 16.0D;
    private static final double FIRST_PERSON_TRANSLATE_Y = 0.0D;
    private static final double FIRST_PERSON_TRANSLATE_Z = -3.0D / 16.0D;
    private static final double THIRD_PERSON_ANIMATED_Y_CORRECTION = -8.75D / 16.0D;
    private static final double FORGE_MODEL_CENTER_OFFSET = 0.5D;
    private static final double FORGE_UNIT = 1.0D / 16.0D;
    private static final double FORGE_GUN_ORIGIN_X = 8.0D;
    private static final double FORGE_GUN_ORIGIN_Y = 0.0D;
    private static final double FORGE_GUN_ORIGIN_Z = 8.0D;
    private static final double OPEN_SIGHT_SCOPE_CAMERA_X = 0.0D;
    private static final double REFLEX_SCOPE_CAMERA_Y = 1.05D;
    private static final double MONOCLE_SCOPE_CAMERA_Y = 0.85D;
    private static final double OPEN_SIGHT_SCOPE_CAMERA_Z = 12.0D;
    private static final double FORGE_SCOPE_Y_OFFSET = 0.54D;
    private static final double FORGE_SCOPE_Z_OFFSET = -0.16D;
    private static final double FORGE_IRON_SIGHT_Y_OFFSET = 0.6059D;
    private static final double FORGE_IRON_SIGHT_Z_OFFSET = -0.16D;
    private static final double FORGE_LEGACY_IRON_SIGHT_Z_OFFSET = 0.72D;
    private static final ResourceLocation REFLEX_SIGHT = Reference.id("reflex_sight");
    private static final ResourceLocation MONOCLE_SIGHT = Reference.id("monocle_sight");
    private static final ResourceLocation HOLOGRAPHIC_SIGHT = Reference.id("holographic_sight");
    private static final ResourceLocation TELESCOPIC_SIGHT = Reference.id("telescopic_sight");
    private static final ResourceLocation SPYGLASS = ResourceLocation.withDefaultNamespace("spyglass");
    private static final Map<ResourceLocation, Map<ResourceLocation, ForgeZoomOffset>> RIFLE_SCOPE_ADS_CORRECTIONS = Map.of(
            Reference.id("combat_rifle"), Map.of(
                    TELESCOPIC_SIGHT, zoom(0.0D, 1.13D, 0.0D),
                    SPYGLASS, zoom(0.0D, -0.68D, 0.0D)
            ),
            Reference.id("service_rifle"), Map.of(
                    TELESCOPIC_SIGHT, zoom(0.0D, 0.55D, 0.0D),
                    SPYGLASS, zoom(0.0D, -0.75D, 0.0D)
            )
    );
    private static final Map<ResourceLocation, ForgeZoomOffset> FORGE_ZOOM_OFFSETS = Map.ofEntries(
            Map.entry(Reference.id("abstract_gun"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("assault_rifle"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("blossom_rifle"), zoom(0.0D, 4.75D, -1.5D)),
            Map.entry(Reference.id("bolt_action_rifle"), zoom(0.0D, 4.35D, -1.25D)),
            Map.entry(Reference.id("burst_rifle"), zoom(0.0D, 4.65D, -1.75D)),
            Map.entry(Reference.id("combat_pistol"), zoom(0.0D, 5.135D, -1.75D)),
            Map.entry(Reference.id("combat_rifle"), zoom(0.0D, 5.71D, -1.75D)),
            Map.entry(Reference.id("custom_smg"), zoom(0.0D, 3.85D, -1.75D)),
            Map.entry(Reference.id("double_barrel_shotgun"), zoom(0.0D, 5.05D, 0.75D)),
            Map.entry(Reference.id("finger_gun"), zoom(0.0D, 3.0D, -1.75D)),
            Map.entry(Reference.id("flamethrower"), zoom(0.0D, 5.71D, -1.75D)),
            Map.entry(Reference.id("flare_gun"), zoom(0.0D, 4.85D, -1.75D)),
            Map.entry(Reference.id("grenade_launcher"), zoom(0.0D, 3.61D, 2.0D)),
            Map.entry(Reference.id("hollenfire_mk2"), zoom(0.0D, 5.2D, -1.75D)),
            Map.entry(Reference.id("holy_shotgun"), zoom(0.0D, 3.14D, -1.25D)),
            Map.entry(Reference.id("hypersonic_cannon"), zoom(0.0D, 3.9D, -2.25D)),
            Map.entry(Reference.id("infantry_rifle"), zoom(0.0D, 3.8D, 1.75D)),
            Map.entry(Reference.id("light_machine_gun"), zoom(0.0D, 4.56D, -1.75D)),
            Map.entry(Reference.id("pump_shotgun"), zoom(0.0D, 3.3D, -1.25D)),
            Map.entry(Reference.id("repeating_shotgun"), zoom(0.0D, 3.75D, -1.75D)),
            Map.entry(Reference.id("revolver"), zoom(0.0D, 3.85D, -1.75D)),
            Map.entry(Reference.id("rocket_launcher"), zoom(2.8D, 3.4D, -1.75D)),
            Map.entry(Reference.id("semi_auto_pistol"), zoom(0.0D, 4.965D, -1.75D)),
            Map.entry(Reference.id("semi_auto_rifle"), zoom(0.0D, 3.6D, -1.25D)),
            Map.entry(Reference.id("service_rifle"), zoom(0.0D, 5.2D, -1.75D)),
            Map.entry(Reference.id("soulhunter_mk2"), zoom(0.0D, 4.415D, 2.0D)),
            Map.entry(Reference.id("subsonic_rifle"), zoom(0.0D, 3.95D, -0.75D)),
            Map.entry(Reference.id("supersonic_shotgun"), zoom(0.0D, 4.2D, -3.75D)),
            Map.entry(Reference.id("typhoonee"), zoom(0.0D, 4.45D, -1.25D)),
            Map.entry(Reference.id("waterpipe_shotgun"), zoom(0.0D, 3.65D, 0.75D))
    );
    private static final Set<String> DEBUG_BONES = ConcurrentHashMap.newKeySet();
    private static Method renderModelListsMethod;
    private static volatile long nextRenderByItemDebugNanos;
    private static volatile long nextRenderDebugNanos;
    private static volatile long nextArmDebugNanos;
    private static volatile long nextGuiDebugNanos;
    private final AnimatedGunGeoModel gunModel;

    public AnimatedGunRenderer() {
        this(new AnimatedGunGeoModel());
    }

    private AnimatedGunRenderer(AnimatedGunGeoModel gunModel) {
        super(gunModel);
        this.gunModel = gunModel;
        this.addRenderLayer(new GunScopeAttachmentLayer(this));
        this.addRenderLayer(new GunBayonetAttachmentLayer(this));
        this.addRenderLayer(new GunPositionedAttachmentLayer(this));
    }

    public ItemDisplayContext currentPerspective() {
        return this.renderPerspective;
    }

    @Override
    public ResourceLocation getTextureLocation(AnimatedGunItem animatable) {
        return GunPaintJobTextures.texture(animatable, this.getCurrentItemStack());
    }

    @Override
    public Color getRenderColor(AnimatedGunItem animatable, float partialTick, int packedLight) {
        return Color.ofOpaque(dyeColor(this.getCurrentItemStack()));
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
        return isSameHeldItem(stack, main) || isSameHeldItem(stack, off);
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
        if (stack != null && isSameHeldItem(stack, main)) {
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
        this.gunModel.setCurrentStack(stack);
        // SW inventory icons: item/generated layer0 textures/item/*_icon (separate_transforms gui perspective).
        if (displayContext == ItemDisplayContext.GUI && renderStaticGuiModel(stack, poseStack, bufferSource, packedLight, packedOverlay)) {
            return;
        }

        long now = System.nanoTime();
        if (now >= nextRenderByItemDebugNanos) {
            nextRenderByItemDebugNanos = now + DEBUG_WINDOW_NANOS;
            // JustEnoughGuns.LOGGER.info(
            //         "[JEG_RENDER_DEBUG] renderByItem item={} perspective={}",
            //         stack.getItem(),
            //         displayContext
            // );
        }
        if (isFirstPersonDisplay(displayContext) && stack.getItem() instanceof AnimatedGunItem gun) {
            if (shouldHideScopedFirstPersonGun(stack, gun)) {
                return;
            }

            poseStack.pushPose();
            try {
                if (!"holy_shotgun".equals(gun.getStats().id().getPath())) {
                    applyFirstPersonAdsTransform(stack, gun, displayContext, poseStack);
                }
                if (!renderForgeSpecialModel(stack, gun, poseStack, bufferSource, packedLight, packedOverlay)) {
                    super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
                }
            } finally {
                poseStack.popPose();
            }
            return;
        }
        if (isThirdPersonDisplay(displayContext) && stack.getItem() instanceof AnimatedGunItem gun) {
            stopThirdPersonAnimations(gun, stack);
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

    private static void stopThirdPersonAnimations(AnimatedGunItem gun, ItemStack stack) {
        var manager = gun.getAnimatableInstanceCache()
                .getManagerForId(software.bernie.geckolib.animatable.GeoItem.getId(stack));
        var controller = manager.getAnimationControllers().get(AnimatedGunItem.CONTROLLER);
        if (controller != null) {
            controller.forceAnimationReset();
            controller.stop();
        }
        manager.clearSnapshotCache();
    }

    private static void applyFirstPersonAdsTransform(ItemStack stack, AnimatedGunItem gun, ItemDisplayContext displayContext, PoseStack poseStack) {
        float ads = AimingHandler.get().getRenderAdsProgress();
        if (ads <= 0.0F) {
            return;
        }

        // SuperbWarfare guided launchers apply ADS on GeckoLib bone "bone" (see renderRecursively).
        if (isSwGuidedLauncher(gun.getStats().id().getPath())) {
            return;
        }

        ItemTransform transform = staticItemFirstPersonTransform(gun.getStats().id(), displayContext);
        double translateX = transform.translation.x();
        double translateY = transform.translation.y();
        double translateZ = transform.translation.z();
        double scaleX = transform.scale.x();
        double scaleY = transform.scale.y();
        double scaleZ = transform.scale.z();

        double transition = easeOutQuad(ads);
        Double openSightCameraY = openSightScopeCameraY(stack);
        ForgeZoomOffset adsOffset = openSightCameraY != null
                ? openSightScopeAdsOffset(gun.getStats().id(), openSightCameraY, translateX, translateY, translateZ, scaleX, scaleY, scaleZ)
                : null;
        if (adsOffset == null) {
            boolean telescopicSight = hasTelescopicSight(stack);
            ForgeZoomOffset zoom = telescopicSight
                    ? zoom(0.0D, 5.0D, -4.4D)
                    : FORGE_ZOOM_OFFSETS.get(gun.getStats().id());
            if (zoom == null) {
                return;
            }
            if (!telescopicSight) {
                zoom = withAttachmentAdsOffset(zoom, GunAttachments.modifiers(stack));
            }
            zoom = withRifleScopeAdsCorrection(stack, gun.getStats().id(), zoom);
            adsOffset = ironSightAdsOffset(zoom, translateX, translateY, translateZ, scaleX, scaleY, scaleZ);
        }

        poseStack.translate(-0.56D * transition, 0.52D * transition, 0.72D * transition);
        poseStack.translate(-adsOffset.xOffset() * transition, -adsOffset.yOffset() * transition, -adsOffset.zOffset() * transition);
    }

    private static ForgeZoomOffset ironSightAdsOffset(ForgeZoomOffset zoom, double translateX, double translateY, double translateZ, double scaleX, double scaleY, double scaleZ) {
        double xOffset = translateX - FORGE_MODEL_CENTER_OFFSET * scaleX
                + FORGE_MODEL_CENTER_OFFSET * scaleX
                - zoom.xOffset() * FORGE_UNIT * scaleX;
        double yOffset = translateY - FORGE_MODEL_CENTER_OFFSET * scaleY
                + zoom.yOffset() * FORGE_UNIT * scaleY
                + FORGE_IRON_SIGHT_Y_OFFSET;
        double zOffset = translateZ - FORGE_MODEL_CENTER_OFFSET * scaleZ
                + FORGE_MODEL_CENTER_OFFSET * scaleZ
                - zoom.zOffset() * FORGE_UNIT * scaleZ
                + FORGE_IRON_SIGHT_Z_OFFSET + FORGE_LEGACY_IRON_SIGHT_Z_OFFSET;
        return zoom(xOffset, yOffset, zOffset);
    }

    private static ForgeZoomOffset openSightScopeAdsOffset(ResourceLocation gunId, double scopeCameraY, double translateX, double translateY, double translateZ, double scaleX, double scaleY, double scaleZ) {
        GunAttachmentTransforms.Transform scopeTransform = GunAttachmentTransforms.transform(gunId, AttachmentType.SCOPE).orElse(null);
        if (scopeTransform == null || !scopeTransform.isVisible()) {
            return null;
        }
        double scopeScale = scopeTransform.scale();
        double xOffset = translateX - FORGE_MODEL_CENTER_OFFSET * scaleX
                + FORGE_GUN_ORIGIN_X * FORGE_UNIT * scaleX
                + scopeTransform.x() * FORGE_UNIT * scaleX
                + OPEN_SIGHT_SCOPE_CAMERA_X * FORGE_UNIT * scaleX * scopeScale;
        double yOffset = translateY - FORGE_MODEL_CENTER_OFFSET * scaleY
                + FORGE_GUN_ORIGIN_Y * FORGE_UNIT * scaleY
                + scopeTransform.y() * FORGE_UNIT * scaleY
                + ((scopeCameraY * FORGE_UNIT * scaleY) + FORGE_SCOPE_Y_OFFSET) * scopeScale;
        double zOffset = translateZ - FORGE_MODEL_CENTER_OFFSET * scaleZ
                + FORGE_GUN_ORIGIN_Z * FORGE_UNIT * scaleZ
                + scopeTransform.z() * FORGE_UNIT * scaleZ
                + ((OPEN_SIGHT_SCOPE_CAMERA_Z * FORGE_UNIT * scaleZ) + FORGE_SCOPE_Z_OFFSET) * scopeScale;
        return zoom(xOffset, yOffset, zOffset);
    }

    private static ItemTransform staticItemFirstPersonTransform(ResourceLocation itemId, ItemDisplayContext displayContext) {
        Minecraft mc = Minecraft.getInstance();
        ModelResourceLocation modelId = new ModelResourceLocation(Reference.id("item/" + itemId.getPath()), "standalone");
        BakedModel model = mc.getModelManager().getModel(modelId);
        if (model == mc.getModelManager().getMissingModel()) {
            return new ItemTransform(
                    new org.joml.Vector3f(),
                    new org.joml.Vector3f((float) FIRST_PERSON_TRANSLATE_X, (float) FIRST_PERSON_TRANSLATE_Y, (float) FIRST_PERSON_TRANSLATE_Z),
                    new org.joml.Vector3f(1.0F, 1.0F, 1.0F)
            );
        }
        return model.getTransforms().getTransform(displayContext);
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
            case "typhoonee" -> renderSpecialModel(Reference.id("special/typhoonee/main"), stack, poseStack, bufferSource, packedLight, packedOverlay);
            default -> false;
        };
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
            // JustEnoughGuns.LOGGER.warn("[JEG_RENDER_DEBUG] special model render failed for {} model={}: {}", stack.getItem(), modelPath, exception.toString());
            return false;
        }
    }

    private static double easeOutQuad(double value) {
        double inverse = 1.0D - Mth.clamp(value, 0.0D, 1.0D);
        return 1.0D - inverse * inverse;
    }

    private record ForgeZoomOffset(double xOffset, double yOffset, double zOffset) {}

    private static ForgeZoomOffset zoom(double xOffset, double yOffset, double zOffset) {
        return new ForgeZoomOffset(xOffset, yOffset, zOffset);
    }

    private static ForgeZoomOffset withAttachmentAdsOffset(ForgeZoomOffset zoom, AttachmentModifiers modifiers) {
        return new ForgeZoomOffset(
                zoom.xOffset() + modifiers.adsViewXOffset(),
                zoom.yOffset() + modifiers.adsViewYOffset(),
                zoom.zOffset() + modifiers.adsViewZOffset()
        );
    }

    private static ForgeZoomOffset withRifleScopeAdsCorrection(ItemStack stack, ResourceLocation gunId, ForgeZoomOffset zoom) {
        Map<ResourceLocation, ForgeZoomOffset> corrections = RIFLE_SCOPE_ADS_CORRECTIONS.get(gunId);
        if (corrections == null) {
            return zoom;
        }
        ResourceLocation scopeId = GunAttachments.id(stack, AttachmentType.SCOPE).orElse(null);
        if (scopeId == null) {
            return zoom;
        }
        ForgeZoomOffset correction = corrections.get(scopeId);
        if (correction != null) {
            return withCorrection(zoom, correction);
        }
        return zoom;
    }

    private static ForgeZoomOffset withCorrection(ForgeZoomOffset zoom, ForgeZoomOffset correction) {
        return new ForgeZoomOffset(
                zoom.xOffset() + correction.xOffset(),
                zoom.yOffset() + correction.yOffset(),
                zoom.zOffset() + correction.zOffset()
        );
    }

    private static boolean hasTelescopicSight(ItemStack stack) {
        return GunScopeSupport.hasTelescopicSight(stack);
    }

    private static Double openSightScopeCameraY(ItemStack stack) {
        ResourceLocation scopeId = GunAttachments.id(stack, AttachmentType.SCOPE).orElse(null);
        if (REFLEX_SIGHT.equals(scopeId)) {
            return REFLEX_SCOPE_CAMERA_Y;
        }
        if (MONOCLE_SIGHT.equals(scopeId) || HOLOGRAPHIC_SIGHT.equals(scopeId)) {
            return MONOCLE_SCOPE_CAMERA_Y;
        }
        return null;
    }

    private static boolean shouldHideScopedFirstPersonGun(ItemStack stack, AnimatedGunItem gun) {
        // SW Javelin/Igla: hide whole first-person model once zoomPos > 0.8 so only scope HUD remains
        ResourceLocation id = gun.getStats().id();
        if (Reference.id("javelin").equals(id) || Reference.id("igla_9k38").equals(id)) {
            return AimingHandler.get().getRenderAdsProgress() > 0.8F;
        }
        return hasTelescopicSight(stack) && AimingHandler.get().getRenderAdsProgress() > 0.5F;
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
        if (!isReRender) {
            ItemStack stack = this.getCurrentItemStack();
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof AnimatedGunItem gun) {
                GunAttachmentVisibility.apply(gun.getStats().id(), stack, bone);
                if ("missile".equals(boneName) && Reference.id("rocket_launcher").equals(gun.getStats().id())) {
                    bone.setHidden(isThirdPersonDisplay(this.renderPerspective));
                }
                // SW javelin/igla: procedural aim (bone) + draw (root). No GeckoLib draw keys in SW.
                if (isSwGuidedLauncher(gun.getStats().id().getPath())) {
                    applySwGuidedLauncherBonePose(this, gun.getStats().id().getPath(), stack, bone, boneName, partialTick);
                }
                debugFirstPersonBones(stack, boneName);
                if ("attachment_bone".equals(boneName)) {
                    renderFirstPersonMuzzleFlashForBone(poseStack, bone, stack, bufferSource);
                }
            }

            boolean armBone = isArmBone(boneName);
            if (armBone) {
                bone.setHidden(true);
                bone.setChildrenHidden(false);
                renderArmForBone(poseStack, bone, boneName, bufferSource, packedLight);
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    private void renderFirstPersonMuzzleFlashForBone(
            PoseStack poseStack,
            GeoBone bone,
            ItemStack stack,
            MultiBufferSource bufferSource
    ) {
        if (!isFirstPersonContext()) {
            return;
        }

        poseStack.pushPose();
        try {
            RenderUtil.translateMatrixToBone(poseStack, bone);
            RenderUtil.translateToPivotPoint(poseStack, bone);
            RenderUtil.rotateMatrixAroundBone(poseStack, bone);
            RenderUtil.scaleMatrixForBone(poseStack, bone);
            RenderUtil.translateAwayFromPivotPoint(poseStack, bone);
            GunClientEvents.renderFirstPersonMuzzleFlashRelativeToBone(
                    poseStack,
                    bufferSource,
                    stack,
                    resolveRenderedHand(),
                    bone.getPivotX(),
                    bone.getPivotY(),
                    bone.getPivotZ()
            );
        } finally {
            poseStack.popPose();
        }
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
        boolean leftBone = isLeftArmBone(boneName);
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
        boolean sameMain = mc.player != null && isSameHeldItem(stack, mc.player.getMainHandItem());
        boolean sameOffhand = mc.player != null && isSameHeldItem(stack, mc.player.getOffhandItem());
        // JustEnoughGuns.LOGGER.info(
        //         "[JEG_RENDER_DEBUG] first-person gun renderer active item={} perspective={} hand={} sameMain={} sameOffhand={} sampledBones={}",
        //         stack.getItem(),
        //         this.renderPerspective,
        //         resolveRenderedHand(),
        //         sameMain,
        //         sameOffhand,
        //         DEBUG_BONES
        // );
        DEBUG_BONES.clear();
    }

    private static void debugArmRender(ItemStack stack, String boneName, HumanoidArm arm, boolean rendered) {
        long now = System.nanoTime();
        if (now < nextArmDebugNanos) {
            return;
        }
        nextArmDebugNanos = now + DEBUG_WINDOW_NANOS;
        // JustEnoughGuns.LOGGER.info(
        //         "[JEG_RENDER_DEBUG] arm bone render item={} bone={} arm={} rendered={}",
        //         stack.getItem(),
        //         boneName,
        //         arm,
        //         rendered
        // );
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
            // JustEnoughGuns.LOGGER.warn("[JEG_RENDER_DEBUG] static GUI model render failed for {}: {}", stack.getItem(), exception.toString());
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
        // JustEnoughGuns.LOGGER.info(
        //         "[JEG_RENDER_DEBUG] gui static model item={} model={} rendered={}",
        //         stack.getItem(),
        //         modelId,
        //         rendered
        // );
    }

    private static boolean isSwGuidedLauncher(String path) {
        return "javelin".equals(path) || "igla_9k38".equals(path);
    }

    /**
     * SuperbWarfare JavelinItemModel / IglaItemModel + gunRootMove draw.
     * Bone positions are GeckoLib/Blockbench units (not world blocks).
     */
    private static void applySwGuidedLauncherBonePose(
            AnimatedGunRenderer renderer,
            String path,
            ItemStack stack,
            GeoBone bone,
            String boneName,
            float partialTick
    ) {
        boolean firstPerson = renderer != null && renderer.isFirstPersonContext();
        float zoomTime = firstPerson ? AimingHandler.get().getRenderAdsProgress() : 0.0F;
        float zoomPos = (float) swEaseInOutQuint(zoomTime);
        float zoomPosZ = (float) swParabola(zoomTime);
        float drawTime = firstPerson ? GunItem.getClientDrawTime(stack, partialTick) : 0.0F;

        if ("bone".equals(boneName)) {
            applySwAimBone(path, bone, zoomPos, zoomPosZ);
            return;
        }
        if ("root".equals(boneName)) {
            applySwDrawRoot(bone, drawTime, zoomTime);
        }
    }

    private static void applySwAimBone(String path, GeoBone bone, float zp, float zpz) {
        if ("javelin".equals(path)) {
            bone.setPosX(1.66F * zp + 0.2F * zpz);
            bone.setPosY(5.5F * zp + 0.8F * zpz);
            bone.setPosZ(15.9F * zp);
            bone.setScaleZ(1.0F - 0.8F * zp);
            bone.setRotZ(-4.75F * Mth.DEG_TO_RAD * zp + 0.02F * zpz);
        } else {
            bone.setPosX(1.66F * zp + 0.2F * zpz);
            bone.setPosY(3.485F * zp - 0.4F * zpz);
            bone.setPosZ(8.10F * zp);
            bone.setScaleZ(1.0F - 0.7F * zp);
            bone.setRotZ(-8.0F * Mth.DEG_TO_RAD * zp + 0.05F * zpz);
        }
        bone.setScaleX(1.0F);
        bone.setScaleY(1.0F);
        bone.setRotX(0.0F);
        bone.setRotY(0.0F);
    }

    /** SW gunRootMove draw component only (raise from below). */
    private static void applySwDrawRoot(GeoBone bone, float drawTime, float zoomTime) {
        float fade = 1.0F - zoomTime;
        bone.setPosX(20.0F * drawTime * fade);
        bone.setPosY(-40.0F * drawTime * fade);
        bone.setPosZ(0.0F);
        bone.setRotX(-60.0F * Mth.DEG_TO_RAD * drawTime * fade);
        bone.setRotY(300.0F * Mth.DEG_TO_RAD * drawTime * fade);
        bone.setRotZ(90.0F * Mth.DEG_TO_RAD * drawTime * fade);
    }

    /** SW AnimationCurves.EASE_IN_OUT_QUINT (implemented as cubic in SW). */
    private static double swEaseInOutQuint(double x) {
        x = Mth.clamp(x, 0.0D, 1.0D);
        return x < 0.5D ? 4.0D * x * x * x : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
    }

    /** SW AnimationCurves.PARABOLA: peaks at 1 when x=0.5. */
    private static double swParabola(double x) {
        x = Mth.clamp(x, 0.0D, 1.0D);
        return -Math.pow(2.0D * x - 1.0D, 2.0D) + 1.0D;
    }

    private static boolean isArmBone(String boneName) {
        // Standard JEG bones + SW guided-launcher bones (Lefthand/Righthand).
        return "left_arm".equals(boneName) || "right_arm".equals(boneName)
                || "fake_left_arm".equals(boneName) || "fake_right_arm".equals(boneName)
                || "Lefthand".equals(boneName) || "Righthand".equals(boneName);
    }

    private static boolean isLeftArmBone(String boneName) {
        if (boneName == null) {
            return false;
        }
        // Case-insensitive: "left_arm", "fake_left_arm", SW "Lefthand"
        return boneName.toLowerCase(java.util.Locale.ROOT).contains("left");
    }

    private static int dyeColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0xFFFFFF;
        }
        return GunAttachments.cosmeticItem(stack, AttachmentType.DYE)
                .filter(DyeItem.class::isInstance)
                .map(DyeItem.class::cast)
                .map(dye -> dye.getDyeColor().getFireworkColor())
                .orElse(0xFFFFFF);
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
