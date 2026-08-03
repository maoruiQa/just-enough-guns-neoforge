package ttv.migami.jeg.client.render.gun;

import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.gun.layer.GunAttachmentLayer;
import ttv.migami.jeg.client.render.gun.layer.GunFirstPersonArmsLayer;
import ttv.migami.jeg.client.screen.AttachmentScreen;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID + ".AnimatedGunRenderer");
    public static final DataTicket<Item> ANIMATED_ITEM = DataTicket.create("jeg:animated_item", Item.class);
    private static final Set<String> VANILLA_FALLBACK_WARNED = ConcurrentHashMap.newKeySet();
    private static volatile boolean VANILLA_FALLBACK_DISABLED = false;
    private static final double THIRD_PERSON_ANIMATED_Y_CORRECTION = -8.75D / 16.0D;
    private static final double THIRD_PERSON_MINIGUN_FORWARD_CORRECTION = 0.65D;
    private static final double THIRD_PERSON_MINIGUN_UP_CORRECTION = 0.1D;
    private static final float ATTACHMENT_PREVIEW_X_ROT = 5.0F;
    private static final float ATTACHMENT_PREVIEW_Y_ROT_SPEED = 1.0F;

    // Store vanilla state so we can fall back to 1.21.10-style (vanilla item model) rendering outside first-person.
    private static final DataTicket<ItemStackRenderState> VANILLA_ITEM_STATE =
            DataTicket.create("jeg:vanilla_item_state", ItemStackRenderState.class);
    public static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("jeg:item_stack", ItemStack.class);
    private static final DataTicket<Boolean> USING_VANILLA_NON_FIRST_PERSON =
            DataTicket.create("jeg:using_vanilla_non_first_person", Boolean.class);
    private static final Set<String> FIRST_PERSON_ARM_BONES =
            Set.of("left_arm", "right_arm", "fake_left_arm", "fake_right_arm", "Lefthand", "Righthand");

    private static final class VanillaStateAccess {
        private static final Field LAYERS;
        private static final Field ACTIVE_LAYER_COUNT;
        private static final Field SPECIAL_RENDERER;
        private static final Field SPECIAL_ARGUMENT;

        static {
            try {
                LAYERS = ItemStackRenderState.class.getDeclaredField("layers");
                LAYERS.setAccessible(true);
                ACTIVE_LAYER_COUNT = ItemStackRenderState.class.getDeclaredField("activeLayerCount");
                ACTIVE_LAYER_COUNT.setAccessible(true);

                SPECIAL_RENDERER = ItemStackRenderState.LayerRenderState.class.getDeclaredField("specialRenderer");
                SPECIAL_RENDERER.setAccessible(true);
                SPECIAL_ARGUMENT = ItemStackRenderState.LayerRenderState.class.getDeclaredField("argumentForSpecialRendering");
                SPECIAL_ARGUMENT.setAccessible(true);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to init ItemStackRenderState reflection access", e);
            }
        }

        private VanillaStateAccess() {}
    }

    public AnimatedGunRenderer() {
        super(new AnimatedGunGeoModel());
        this.withRenderLayer(new GunAttachmentLayer(this));
        // Render player-skin arms in first-person, driven by GeckoLib arm bones in the gun animations.
        this.withRenderLayer(new GunFirstPersonArmsLayer(this));
    }

    @Override
    public GeoRenderState createRenderState(AnimatedGunItem animatable, GeoItemRenderer.RenderData context) {
        GeoRenderState state = super.createRenderState(animatable, context);
        ItemStack rawStack = context != null ? context.itemStack() : ItemStack.EMPTY;
        ItemDisplayContext rawPerspective = context != null ? context.renderPerspective() : ItemDisplayContext.NONE;
        ItemStack stack = resolveStableStack(rawStack, animatable);
        // Ensure perspective is always present; without it we fall back to NONE and handheld renders look like GUI/ground.
        ItemDisplayContext perspective = resolveStableContext(
                rawPerspective,
                stack
        );
        state.addGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        state.addGeckolibData(ANIMATED_ITEM, animatable);
        state.addGeckolibData(ITEM_STACK, stack);
        if (context != null) {
            state.addGeckolibData(VANILLA_ITEM_STATE, context.vanillaRenderState());
        }
        state.addGeckolibData(USING_VANILLA_NON_FIRST_PERSON, shouldUseVanillaNonFirstPerson(perspective, state.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null), state, animatable.getStats().id().getPath()));
        return state;
    }

    @Override
    public void captureDefaultRenderState(AnimatedGunItem animatable, GeoItemRenderer.RenderData context, GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);

        // GeckoLib's SpecialModelRenderer pipeline carries the perspective in GeoItemRenderer.RenderData,
        // but it does not guarantee DataTickets.ITEM_RENDER_PERSPECTIVE is populated.
        // If it is missing we fall back to NONE, which makes handheld items render as if they were GUI/ground,
        // producing the "floating upper-left" placement in third person.
        ItemStack rawStack = context != null ? context.itemStack() : ItemStack.EMPTY;
        ItemDisplayContext rawPerspective = context != null ? context.renderPerspective() : ItemDisplayContext.NONE;
        ItemStack stack = resolveStableStack(rawStack, animatable);
        ItemDisplayContext perspective = resolveStableContext(
                rawPerspective,
                stack
        );
        renderState.addGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        renderState.addGeckolibData(ANIMATED_ITEM, animatable);
        renderState.addGeckolibData(ITEM_STACK, stack);
        if (context != null) {
            renderState.addGeckolibData(VANILLA_ITEM_STATE, context.vanillaRenderState());
        }
        renderState.addGeckolibData(USING_VANILLA_NON_FIRST_PERSON, shouldUseVanillaNonFirstPerson(perspective, renderState.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null), renderState, animatable.getStats().id().getPath()));
    }

    @Override
    public void submit(GeoRenderState renderState, com.mojang.blaze3d.vertex.PoseStack poseStack, SubmitNodeCollector collector, int glowColour) {
        ItemDisplayContext ctx = resolveStableContext(renderState);
        String gunPath = gunPathFromRenderState(renderState);
        if (isFirstPerson(ctx) && shouldHideScopedFirstPersonGun(renderState)) {
            return;
        }
        // Third-person uses GeckoLib for static Geo pose and attachment layers. GUI/inventory keeps the
        // vanilla item model fallback so static item/gui models render normally.
        boolean animatedNonFirstPerson = shouldUseAnimatedNonFirstPerson(ctx, renderState, gunPath);
        ItemStackRenderState vanilla = renderState.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null);
        boolean vanillaEmpty = vanilla == null || vanilla.isEmpty();
        if (!isFirstPerson(ctx) && !VANILLA_FALLBACK_DISABLED && !animatedNonFirstPerson) {
            if (vanilla != null && !vanilla.isEmpty()) {
                int packedLight = renderState.getOrDefaultGeckolibData(DataTickets.PACKED_LIGHT, 0);
                int packedOverlay = renderState.getOrDefaultGeckolibData(DataTickets.PACKED_OVERLAY, 0);
                String ctxName = ctx.name();

                // The vanilla render state for GeoItems contains a SpecialModelRenderer layer that routes back
                // into GeckoLib (and thus back into this renderer), causing infinite recursion/StackOverflow.
                // For non-first-person contexts we want the "base" baked-quads model, so temporarily strip
                // special rendering from all active layers before submitting.
                ItemStackRenderState.LayerRenderState[] layers;
                int count;
                try {
                    layers = (ItemStackRenderState.LayerRenderState[]) VanillaStateAccess.LAYERS.get(vanilla);
                    count = (int) VanillaStateAccess.ACTIVE_LAYER_COUNT.get(vanilla);
                } catch (IllegalAccessException e) {
                    String warnKey = "reflect_access|" + gunPath + "|" + ctxName;
                    if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                        LOGGER.warn("Vanilla fallback reflection access failed for gun={} context={}; using GeckoLib fallback", gunPath, ctx, e);
                    }
                    super.submit(renderState, poseStack, collector, glowColour);
                    return;
                }

                if (layers == null || count <= 0 || count > layers.length) {
                    String warnKey = "invalid_layers|" + gunPath + "|" + ctxName + "|" + count;
                    if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                        LOGGER.warn(
                                "Vanilla fallback layer layout invalid for gun={} context={} (layers={}, active={}): using GeckoLib fallback",
                                gunPath, ctx, layers == null ? -1 : layers.length, count
                        );
                    }
                    super.submit(renderState, poseStack, collector, glowColour);
                    return;
                }

                Object[] specialRenderers = new Object[count];
                Object[] specialArgs = new Object[count];
                try {
                    for (int i = 0; i < count; i++) {
                        ItemStackRenderState.LayerRenderState layer = layers[i];
                        specialRenderers[i] = VanillaStateAccess.SPECIAL_RENDERER.get(layer);
                        specialArgs[i] = VanillaStateAccess.SPECIAL_ARGUMENT.get(layer);
                        VanillaStateAccess.SPECIAL_RENDERER.set(layer, null);
                        VanillaStateAccess.SPECIAL_ARGUMENT.set(layer, null);
                    }
                } catch (IllegalAccessException e) {
                    String warnKey = "strip_failed|" + gunPath + "|" + ctxName;
                    if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                        LOGGER.warn("Vanilla fallback strip failed for gun={} context={}; using GeckoLib fallback", gunPath, ctx, e);
                    }
                    super.submit(renderState, poseStack, collector, glowColour);
                    return;
                }

                boolean restoreFailed = false;
                try {
                    vanilla.submit(poseStack, collector, packedLight, packedOverlay, glowColour);
                } finally {
                    try {
                        for (int i = 0; i < count; i++) {
                            ItemStackRenderState.LayerRenderState layer = layers[i];
                            VanillaStateAccess.SPECIAL_RENDERER.set(layer, specialRenderers[i]);
                            VanillaStateAccess.SPECIAL_ARGUMENT.set(layer, specialArgs[i]);
                        }
                    } catch (IllegalAccessException e) {
                        restoreFailed = true;
                        String warnKey = "restore_failed|" + gunPath + "|" + ctxName;
                        if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                            LOGGER.error("Vanilla fallback restore failed for gun={} context={}; disabling vanilla fallback", gunPath, ctx, e);
                        }
                    }
                }

                if (restoreFailed) {
                    VANILLA_FALLBACK_DISABLED = true;
                }
                return;
            }

            String warnKey = "missing_vanilla|" + gunPath + "|" + ctx.name();
            if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                LOGGER.warn("Missing vanilla render state for gun={} context={}; using GeckoLib fallback", gunPath, ctx);
            }
        } else if (!isFirstPerson(ctx) && VANILLA_FALLBACK_DISABLED) {
            String warnKey = "vanilla_disabled|" + gunPath;
            if (VANILLA_FALLBACK_WARNED.add(warnKey)) {
                LOGGER.warn("Vanilla non-first-person fallback disabled; using GeckoLib fallback for gun={}", gunPath);
            }
        }

        super.submit(renderState, poseStack, collector, glowColour);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> passInfo) {
        GeoRenderState renderState = passInfo.renderState();
        ItemDisplayContext ctx = resolveStableContext(renderState);
        ItemStackRenderState vanilla = renderState.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null);
        String gunPath = gunPathFromRenderState(renderState);
        if (!VANILLA_FALLBACK_DISABLED && shouldUseVanillaNonFirstPerson(ctx, vanilla, renderState, gunPath)) {
            // Vanilla submit path already applies model display transforms; do not stack GeckoLib transforms on top.
            return;
        }

        // Vanilla first_person ItemTransform is already on the stack. Guided launchers use the same
        // SW/1.21.1 first_person display JSON (raw v4 values); GeckoLib only centers.
        // SW javelin/igla must NOT get GunPoseProfile hand transforms (see ItemInHandRendererMixin).
        super.adjustRenderPose(passInfo);

        if (isFirstPerson(ctx) || (isSwGuidedLauncher(gunPath) && isClientFirstPersonCamera())) {
            HumanoidArm arm = resolveFirstPersonArm(ctx);
            FabricClientBootstrap.captureFirstPersonGunPose(arm, new Matrix4f(passInfo.poseStack().last().pose()));
            return;
        }

        if (isAttachmentScreenPreview(ctx)) {
            applyAttachmentScreenPreviewTransform(passInfo.poseStack());
            return;
        }

        if (isThirdPerson(ctx)) {
            applyThirdPersonAnimatedTransform(gunPath, passInfo.poseStack());
        }
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
        GeoRenderState renderState = passInfo.renderState();
        Identifier gunId = Reference.id("abstract_gun");
        if (renderState.hasGeckolibData(ANIMATED_ITEM)) {
            Item item = renderState.getGeckolibData(ANIMATED_ITEM);
            if (item instanceof AnimatedGunItem gun) {
                gunId = gun.getStats().id();
            }
        }

        Identifier finalGunId = gunId;
        ItemStack gunStack = renderState.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        ItemDisplayContext ctx = resolveStableContext(renderState);
        passInfo.addBoneUpdater((info, snapshots) -> GunAttachmentVisibility.apply(finalGunId, gunStack, snapshots));
        if (isThirdPerson(ctx) && Reference.id("rocket_launcher").equals(finalGunId)) {
            passInfo.addBoneUpdater((info, snapshots) ->
                    snapshots.ifPresent("missile", snapshot -> snapshot.skipRender(true))
            );
        }
        if (isFirstPerson(ctx)) {
            passInfo.addBoneUpdater((info, snapshots) ->
                    FIRST_PERSON_ARM_BONES.forEach(name -> snapshots.ifPresent(name, snapshot -> {
                        snapshot.skipRender(true);
                        snapshot.skipChildrenRender(false);
                    }))
            );
            // SW javelin/igla: aim on "bone" + draw on "root" (1.21.1 / SW JavelinItemModel data).
            String guidedPath = finalGunId.getPath();
            if (isSwGuidedLauncher(guidedPath)) {
                ItemStack guidedStack = gunStack;
                passInfo.addBoneUpdater((info, snapshots) -> applySwGuidedLauncherSnapshots(guidedPath, guidedStack, snapshots));
            }
        } else if (shouldUseAnimatedThirdPerson(ctx, finalGunId.getPath()) || isAttachmentScreenPreview(ctx)) {
            passInfo.addBoneUpdater((info, snapshots) ->
                    FIRST_PERSON_ARM_BONES.forEach(name -> snapshots.ifPresent(name, snapshot -> {
                        snapshot.skipRender(true);
                        snapshot.skipChildrenRender(true);
                    }))
            );
        }

        super.preRenderPass(passInfo, collector);
    }

    private static String gunPathFromRenderState(GeoRenderState renderState) {
        return gunIdFromRenderState(renderState);
    }

    private static String gunIdFromRenderState(GeoRenderState renderState) {
        if (renderState.hasGeckolibData(ANIMATED_ITEM)) {
            Item item = renderState.getGeckolibData(ANIMATED_ITEM);
            if (item instanceof AnimatedGunItem gun) {
                return gun.getStats().id().getPath();
            }
        }
        ItemStack stack = renderState.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof AnimatedGunItem gun) {
            return gun.getStats().id().getPath();
        }
        return "abstract_gun";
    }

    private static boolean shouldHideScopedFirstPersonGun(GeoRenderState renderState) {
        if (!renderState.hasGeckolibData(ANIMATED_ITEM)) {
            return false;
        }

        Item item = renderState.getGeckolibData(ANIMATED_ITEM);
        if (!(item instanceof AnimatedGunItem gun)) {
            return false;
        }

        ItemStack stack = renderState.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);

        // SW Javelin/Igla: hide mesh once ADS is deep so only scope HUD remains.
        // Keep visible while reloading/drawing so those animations can still play under partial ADS.
        Identifier id = gun.getStats().id();
        if (Reference.id("javelin").equals(id) || Reference.id("igla_9k38").equals(id)) {
            if (isLauncherBusyAnimating(stack)) {
                return false;
            }
            return AimingHandler.get().getRenderAdsProgress() > 0.8F;
        }

        return GunScopeSupport.hasTelescopicSight(stack)
                && AimingHandler.get().getRenderAdsProgress() > 0.5F;
    }

    private static boolean isLauncherBusyAnimating(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int reload = stack.getOrDefault(ttv.migami.jeg.init.ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get(), 0);
        int draw = stack.getOrDefault(ttv.migami.jeg.init.ModDataComponents.GUN_DRAW_TICKS_REMAINING.get(), 0);
        return reload > 0 || draw > 0;
    }

    private static boolean isSwGuidedLauncher(String path) {
        return "javelin".equals(path) || "igla_9k38".equals(path);
    }

    private static HumanoidArm resolveFirstPersonArm(ItemDisplayContext ctx) {
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return HumanoidArm.LEFT;
        }
        if (ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return HumanoidArm.RIGHT;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && mc.player.getMainArm() == HumanoidArm.LEFT) {
            return HumanoidArm.LEFT;
        }
        return HumanoidArm.RIGHT;
    }

    private static boolean isClientFirstPersonCamera() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.options != null && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * SuperbWarfare JavelinItemModel / IglaItemModel aim + gunRootMove draw (v4 data on v5 snapshots).
     */
    private static void applySwGuidedLauncherSnapshots(
            String path,
            ItemStack stack,
            com.geckolib.renderer.base.BoneSnapshots snapshots
    ) {
        float zoomTime = AimingHandler.get().getRenderAdsProgress();
        float zoomPos = (float) swEaseInOutQuint(zoomTime);
        float zoomPosZ = (float) swParabola(zoomTime);
        float partialTick = 1.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        }
        float drawTime = GunItem.getClientDrawTime(stack, partialTick);

        snapshots.ifPresent("bone", snap -> applySwAimSnapshot(path, snap, zoomPos, zoomPosZ));
        snapshots.ifPresent("root", snap -> applySwDrawRootSnapshot(snap, drawTime, zoomTime));
    }

    private static void applySwAimSnapshot(
            String path,
            com.geckolib.animation.state.BoneSnapshot snap,
            float zp,
            float zpz
    ) {
        if ("javelin".equals(path)) {
            snap.setTranslation(1.66F * zp + 0.2F * zpz, 5.5F * zp + 0.8F * zpz, 15.9F * zp);
            snap.setScale(1.0F, 1.0F, 1.0F - 0.8F * zp);
            snap.setRotation(0.0F, 0.0F, -4.75F * ((float) Math.PI / 180.0F) * zp + 0.02F * zpz);
        } else {
            snap.setTranslation(1.66F * zp + 0.2F * zpz, 3.485F * zp - 0.4F * zpz, 8.10F * zp);
            snap.setScale(1.0F, 1.0F, 1.0F - 0.7F * zp);
            snap.setRotation(0.0F, 0.0F, -8.0F * ((float) Math.PI / 180.0F) * zp + 0.05F * zpz);
        }
    }

    /** SW gunRootMove draw component only (same as 1.21.1). */
    private static void applySwDrawRootSnapshot(
            com.geckolib.animation.state.BoneSnapshot snap,
            float drawTime,
            float zoomTime
    ) {
        float fade = 1.0F - zoomTime;
        float deg = (float) Math.PI / 180.0F;
        snap.setTranslation(20.0F * drawTime * fade, -40.0F * drawTime * fade, 0.0F);
        snap.setRotation(
                -60.0F * deg * drawTime * fade,
                300.0F * deg * drawTime * fade,
                90.0F * deg * drawTime * fade
        );
    }

    /** SW AnimationCurves.EASE_IN_OUT_QUINT (implemented as cubic in SW). */
    private static double swEaseInOutQuint(double x) {
        x = Math.max(0.0D, Math.min(1.0D, x));
        return x < 0.5D ? 4.0D * x * x * x : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
    }

    /** SW AnimationCurves.PARABOLA: peaks at 1 when x=0.5. */
    private static double swParabola(double x) {
        x = Math.max(0.0D, Math.min(1.0D, x));
        return -Math.pow(2.0D * x - 1.0D, 2.0D) + 1.0D;
    }

    private static ItemDisplayContext resolveStableContext(GeoRenderState renderState) {
        ItemDisplayContext base = renderState.getOrDefaultGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, ItemDisplayContext.NONE);
        ItemStack stack = renderState.getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);
        ItemDisplayContext resolved = resolveStableContext(base, stack);
        if (resolved != ItemDisplayContext.NONE) {
            return resolved;
        }

        // GeckoLib can occasionally provide an empty ITEM_STACK during held-item special rendering.
        // Fall back to the animatable item type so held renders keep a stable hand context.
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return ItemDisplayContext.NONE;
        }
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        Item animItem = renderState.getOrDefaultGeckolibData(ANIMATED_ITEM, (Item) null);
        if (animItem == null) {
            return ItemDisplayContext.NONE;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == animItem) {
            return mainHandContext(mc.player.getMainArm(), firstPerson);
        }

        ItemStack offHand = mc.player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() == animItem) {
            return offHandContext(mc.player.getMainArm(), firstPerson);
        }

        return ItemDisplayContext.NONE;
    }

    private static ItemStack resolveStableStack(ItemStack base, Item animatable) {
        if (base != null && !base.isEmpty()) {
            return base;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || animatable == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.getItem() == animatable) {
            return mainHand;
        }

        ItemStack offHand = mc.player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.getItem() == animatable) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static ItemDisplayContext resolveStableContext(ItemDisplayContext base, ItemStack stack) {
        if (base != null && base != ItemDisplayContext.NONE) {
            return base;
        }
        if (stack == null || stack.isEmpty()) {
            return ItemDisplayContext.NONE;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return ItemDisplayContext.NONE;
        }
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        ItemStack mainHand = mc.player.getMainHandItem();
        ItemStack offHand = mc.player.getOffhandItem();
        if (matchesHeldStack(stack, mainHand)) {
            return mainHandContext(mc.player.getMainArm(), firstPerson);
        }
        if (matchesHeldStack(stack, offHand)) {
            return offHandContext(mc.player.getMainArm(), firstPerson);
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
        if (ItemStack.isSameItemSameComponents(renderStack, heldStack)) {
            return true;
        }
        // Components can drift between client snapshots (ammo/reload flags), which can cause
        // first-person context to flap and produce per-frame model flicker.
        return ItemStack.isSameItem(renderStack, heldStack);
    }

    private static ItemDisplayContext mainHandContext(HumanoidArm mainArm, boolean firstPerson) {
        if (firstPerson) {
            return mainArm == HumanoidArm.LEFT
                    ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        }
        return mainArm == HumanoidArm.LEFT
                ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static ItemDisplayContext offHandContext(HumanoidArm mainArm, boolean firstPerson) {
        if (firstPerson) {
            return mainArm == HumanoidArm.LEFT
                    ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        }
        return mainArm == HumanoidArm.LEFT
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static boolean isFirstPerson(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static boolean isThirdPerson(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static boolean shouldUseAnimatedThirdPerson(ItemDisplayContext ctx, String gunPath) {
        return isThirdPerson(ctx);
    }

    private static boolean shouldUseAnimatedNonFirstPerson(ItemDisplayContext ctx, GeoRenderState renderState, String gunPath) {
        if (shouldUseAnimatedThirdPerson(ctx, gunPath)) {
            return true;
        }
        return isAttachmentScreenPreview(ctx);
    }

    private static boolean isAttachmentScreenPreview(ItemDisplayContext ctx) {
        Minecraft minecraft = Minecraft.getInstance();
        return ctx == ItemDisplayContext.FIXED && minecraft != null && minecraft.gui.screen() instanceof AttachmentScreen;
    }

    private static void applyThirdPersonAnimatedTransform(String gunPath, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        if ("minigun".equals(gunPath)) {
            poseStack.translate(0.0D, THIRD_PERSON_ANIMATED_Y_CORRECTION + THIRD_PERSON_MINIGUN_UP_CORRECTION, THIRD_PERSON_MINIGUN_FORWARD_CORRECTION);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            return;
        }
        poseStack.translate(0.0D, THIRD_PERSON_ANIMATED_Y_CORRECTION, 0.0D);
    }

    private static boolean shouldUseVanillaNonFirstPerson(ItemDisplayContext ctx, ItemStackRenderState vanilla, GeoRenderState renderState, String gunPath) {
        return !isFirstPerson(ctx) && !shouldUseAnimatedNonFirstPerson(ctx, renderState, gunPath) && vanilla != null && !vanilla.isEmpty();
    }

    private static void applyAttachmentScreenPreviewTransform(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        float ticks = minecraft != null && minecraft.player != null
                ? minecraft.player.tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)
                : 0.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(ATTACHMENT_PREVIEW_X_ROT));
        poseStack.mulPose(Axis.YP.rotationDegrees(ticks * ATTACHMENT_PREVIEW_Y_ROT_SPEED));
    }
}
