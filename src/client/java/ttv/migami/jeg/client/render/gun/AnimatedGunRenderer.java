package ttv.migami.jeg.client.render.gun;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.Map;
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
import ttv.migami.jeg.client.render.gun.layer.GunBuiltinScopeLayer;
import ttv.migami.jeg.client.render.gun.layer.GunFirstPersonArmsLayer;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID + ".AnimatedGunRenderer");
    private static final Gson GSON = new Gson();
    public static final DataTicket<Item> ANIMATED_ITEM = DataTicket.create("jeg:animated_item", Item.class);
    private static final Map<String, Map<String, Transform>> TRANSFORMS_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> VANILLA_FALLBACK_WARNED = ConcurrentHashMap.newKeySet();
    private static volatile boolean VANILLA_FALLBACK_DISABLED = false;
    private static final double THIRD_PERSON_MINIGUN_X_CORRECTION = 0.0D;
    private static final double THIRD_PERSON_MINIGUN_Y_CORRECTION = -0.28D;
    private static final double THIRD_PERSON_MINIGUN_FORWARD_CORRECTION = 0.7D;

    // Store vanilla state so we can fall back to 1.21.10-style (vanilla item model) rendering outside first-person.
    private static final DataTicket<ItemStackRenderState> VANILLA_ITEM_STATE =
            DataTicket.create("jeg:vanilla_item_state", ItemStackRenderState.class);
    public static final DataTicket<ItemStack> ITEM_STACK =
            DataTicket.create("jeg:item_stack", ItemStack.class);
    private static final DataTicket<Boolean> USING_VANILLA_NON_FIRST_PERSON =
            DataTicket.create("jeg:using_vanilla_non_first_person", Boolean.class);
    private static final Set<String> FIRST_PERSON_ARM_BONES =
            Set.of("left_arm", "right_arm", "fake_left_arm", "fake_right_arm");

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

    private record Transform(float rotX, float rotY, float rotZ, float tx, float ty, float tz, float sx, float sy, float sz) {
    }

    public AnimatedGunRenderer() {
        super(new AnimatedGunGeoModel());
        this.withRenderLayer(new GunBuiltinScopeLayer(this));
        // Render player-skin arms in first-person, driven by GeckoLib arm bones in the gun animations.
        this.withRenderLayer(new GunFirstPersonArmsLayer(this));
    }

    @Override
    public GeoRenderState createRenderState(AnimatedGunItem animatable, GeoItemRenderer.RenderData context) {
        GeoRenderState state = super.createRenderState(animatable, context);
        // Ensure perspective is always present; without it we fall back to NONE and handheld renders look like GUI/ground.
        ItemDisplayContext perspective = resolveStableContext(
                context != null ? context.renderPerspective() : ItemDisplayContext.NONE,
                context != null ? context.itemStack() : ItemStack.EMPTY
        );
        state.addGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        state.addGeckolibData(ANIMATED_ITEM, animatable);
        if (context != null) {
            state.addGeckolibData(VANILLA_ITEM_STATE, context.vanillaRenderState());
            state.addGeckolibData(ITEM_STACK, context.itemStack());
        }
        state.addGeckolibData(USING_VANILLA_NON_FIRST_PERSON, shouldUseVanillaNonFirstPerson(perspective, state.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null), animatable.getStats().id().getPath()));
        return state;
    }

    @Override
    public void captureDefaultRenderState(AnimatedGunItem animatable, GeoItemRenderer.RenderData context, GeoRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);

        // GeckoLib's SpecialModelRenderer pipeline carries the perspective in GeoItemRenderer.RenderData,
        // but it does not guarantee DataTickets.ITEM_RENDER_PERSPECTIVE is populated.
        // If it is missing we fall back to NONE, which makes handheld items render as if they were GUI/ground,
        // producing the "floating upper-left" placement in third person.
        ItemDisplayContext perspective = resolveStableContext(
                context != null ? context.renderPerspective() : ItemDisplayContext.NONE,
                context != null ? context.itemStack() : ItemStack.EMPTY
        );
        renderState.addGeckolibData(DataTickets.ITEM_RENDER_PERSPECTIVE, perspective);
        renderState.addGeckolibData(ANIMATED_ITEM, animatable);
        if (context != null) {
            renderState.addGeckolibData(VANILLA_ITEM_STATE, context.vanillaRenderState());
            renderState.addGeckolibData(ITEM_STACK, context.itemStack());
        }
        renderState.addGeckolibData(USING_VANILLA_NON_FIRST_PERSON, shouldUseVanillaNonFirstPerson(perspective, renderState.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null), animatable.getStats().id().getPath()));
    }

    @Override
    public void submit(GeoRenderState renderState, com.mojang.blaze3d.vertex.PoseStack poseStack, SubmitNodeCollector collector, int glowColour) {
        ItemDisplayContext ctx = resolveStableContext(renderState);
        String gunPath = gunPathFromRenderState(renderState);
        if (isFirstPerson(ctx) && shouldHideScopedFirstPersonGun(renderState)) {
            return;
        }

        // Use GeckoLib for first-person and minigun third-person. Everything else should render like NeoForge 1.21.10
        // (vanilla item model + its "display" transforms), which fixes the broken inventory/GUI/third-person renders.
        if (!isFirstPerson(ctx) && !VANILLA_FALLBACK_DISABLED && !shouldUseAnimatedThirdPerson(ctx, gunPath)) {
            ItemStackRenderState vanilla = renderState.getOrDefaultGeckolibData(VANILLA_ITEM_STATE, (ItemStackRenderState) null);
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
        if (!VANILLA_FALLBACK_DISABLED && shouldUseVanillaNonFirstPerson(ctx, vanilla, gunPath)) {
            // Vanilla submit path already applies model display transforms; do not stack GeckoLib transforms on top.
            return;
        }

        // Always apply GeckoLib centering as the base transform.
        super.adjustRenderPose(passInfo);

        // First-person pose is controlled by IClientItemExtensions.applyForgeHandTransform (1.21.10-style),
        // and re-applying JSON display transforms here will fight that pipeline.
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            HumanoidArm arm = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
            FabricClientBootstrap.captureFirstPersonGunPose(arm, new Matrix4f(passInfo.poseStack().last().pose()));
            return;
        }

        String gunId = "abstract_gun";
        if (renderState.hasGeckolibData(ANIMATED_ITEM)) {
            Item item = renderState.getGeckolibData(ANIMATED_ITEM);
            if (item instanceof AnimatedGunItem gun) {
                gunId = gun.getStats().id().getPath();
            }
        }

        Transform t = getDisplayTransform(gunId, ctx);
        if (t == null) {
            return;
        }

        boolean leftHanded = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        float tx = t.tx;
        float ty = t.ty;
        float tz = t.tz;
        float rx = t.rotX;
        float ry = t.rotY;
        float rz = t.rotZ;

        // Mirror right-hand transforms for left-hand contexts when no explicit left-hand transform exists.
        if (leftHanded && !hasExplicitLeftTransform(gunId, ctx)) {
            tx = -tx;
            ry = -ry;
            rz = -rz;
        }

        // Model JSON translations are in "pixels" (1/16 block).
        passInfo.poseStack().translate(tx * 0.0625F, ty * 0.0625F, tz * 0.0625F);
        // Match vanilla ItemTransform rotation order (X then Y then Z).
        // PoseStack multiplies the current matrix, so we apply in reverse here.
        passInfo.poseStack().mulPose(Axis.ZP.rotationDegrees(rz));
        passInfo.poseStack().mulPose(Axis.YP.rotationDegrees(ry));
        passInfo.poseStack().mulPose(Axis.XP.rotationDegrees(rx));
        passInfo.poseStack().scale(t.sx, t.sy, t.sz);

        if (isThirdPerson(ctx) && "minigun".equals(gunId)) {
            applyThirdPersonMinigunTransform(passInfo.poseStack());
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
        ItemDisplayContext ctx = resolveStableContext(renderState);
        passInfo.addBoneUpdater((info, snapshots) -> GunAttachmentVisibility.apply(finalGunId, snapshots));
        if (isFirstPerson(ctx)) {
            passInfo.addBoneUpdater((info, snapshots) ->
                    FIRST_PERSON_ARM_BONES.forEach(name -> snapshots.ifPresent(name, snapshot -> {
                        snapshot.skipRender(true);
                        snapshot.skipChildrenRender(false);
                    }))
            );
        } else if (shouldUseAnimatedThirdPerson(ctx, finalGunId.getPath())) {
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
        if (renderState.hasGeckolibData(ANIMATED_ITEM)) {
            Item item = renderState.getGeckolibData(ANIMATED_ITEM);
            if (item instanceof AnimatedGunItem gun) {
                return gun.getStats().id().getPath();
            }
        }
        return "unknown";
    }

    private static boolean shouldHideScopedFirstPersonGun(GeoRenderState renderState) {
        if (!renderState.hasGeckolibData(ANIMATED_ITEM)) {
            return false;
        }

        Item item = renderState.getGeckolibData(ANIMATED_ITEM);
        if (!(item instanceof AnimatedGunItem gun)) {
            return false;
        }

        return Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled()
                && AimingHandler.get().getNormalisedAdsProgress() > 0.5F;
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

    private static boolean hasExplicitLeftTransform(String gunId, ItemDisplayContext ctx) {
        String key = contextKey(ctx);
        if (key == null || !key.endsWith("_lefthand")) {
            return false;
        }
        Map<String, Transform> byKey = TRANSFORMS_CACHE.computeIfAbsent(gunId, AnimatedGunRenderer::loadTransformsForGun);
        if (byKey.containsKey(key)) {
            return true;
        }
        if (!"abstract_gun".equals(gunId)) {
            Map<String, Transform> fallback = TRANSFORMS_CACHE.computeIfAbsent("abstract_gun", AnimatedGunRenderer::loadTransformsForGun);
            return fallback.containsKey(key);
        }
        return false;
    }

    private static boolean isFirstPerson(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    private static boolean isThirdPerson(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private static boolean shouldUseAnimatedThirdPerson(ItemDisplayContext ctx, String gunPath) {
        return isThirdPerson(ctx) && "minigun".equals(gunPath);
    }

    private static void applyThirdPersonMinigunTransform(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.translate(THIRD_PERSON_MINIGUN_X_CORRECTION, THIRD_PERSON_MINIGUN_Y_CORRECTION, THIRD_PERSON_MINIGUN_FORWARD_CORRECTION);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
    }

    private static boolean shouldUseVanillaNonFirstPerson(ItemDisplayContext ctx, ItemStackRenderState vanilla, String gunPath) {
        return !isFirstPerson(ctx) && !shouldUseAnimatedThirdPerson(ctx, gunPath) && vanilla != null && !vanilla.isEmpty();
    }

    private static Transform getDisplayTransform(String gunId, ItemDisplayContext ctx) {
        String key = contextKey(ctx);
        if (key == null) {
            return null;
        }

        Map<String, Transform> byKey = TRANSFORMS_CACHE.computeIfAbsent(gunId, AnimatedGunRenderer::loadTransformsForGun);
        Transform t = byKey.get(key);
        if (t != null) {
            return t;
        }

        // If left-hand transform isn't defined, fall back to right-hand and mirror in code.
        if (key.endsWith("_lefthand")) {
            String rightKey = key.replace("_lefthand", "_righthand");
            t = byKey.get(rightKey);
            if (t != null) {
                return t;
            }
        }

        // Fallback to abstract_gun if specific gun json missing.
        if (!"abstract_gun".equals(gunId)) {
            Map<String, Transform> fallback = TRANSFORMS_CACHE.computeIfAbsent("abstract_gun", AnimatedGunRenderer::loadTransformsForGun);
            t = fallback.get(key);
            if (t != null) {
                return t;
            }
            if (key.endsWith("_lefthand")) {
                String rightKey = key.replace("_lefthand", "_righthand");
                return fallback.get(rightKey);
            }
        }

        return null;
    }

    private static String contextKey(ItemDisplayContext ctx) {
        return switch (ctx) {
            case GUI -> "gui";
            case FIXED -> "fixed";
            case GROUND -> "ground";
            case HEAD -> "head";
            case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
            case FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand";
            case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
            case THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand";
            default -> null;
        };
    }

    private static Map<String, Transform> loadTransformsForGun(String gunId) {
        Map<String, Transform> out = new ConcurrentHashMap<>();
        var modelId = Reference.id("models/item/" + gunId + ".json");

        try {
            var opt = Minecraft.getInstance().getResourceManager().getResource(modelId);
            if (opt.isEmpty()) {
                return out;
            }

            try (var in = opt.get().open();
                 var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                JsonObject display = root != null && root.has("display") && root.get("display").isJsonObject()
                        ? root.getAsJsonObject("display")
                        : null;
                if (display == null) {
                    return out;
                }

                for (Map.Entry<String, JsonElement> e : display.entrySet()) {
                    if (!e.getValue().isJsonObject()) {
                        continue;
                    }
                    out.put(e.getKey(), parseTransform(e.getValue().getAsJsonObject()));
                }
            }
        } catch (Exception ignored) {
            // If anything goes wrong, fall back to GeckoLib's base pose.
        }

        return out;
    }

    private static Transform parseTransform(JsonObject obj) {
        float rx = 0, ry = 0, rz = 0;
        float tx = 0, ty = 0, tz = 0;
        float sx = 1, sy = 1, sz = 1;

        if (obj.has("rotation")) {
            float[] r = readVec3(obj.get("rotation"), 0, 0, 0);
            rx = r[0];
            ry = r[1];
            rz = r[2];
        }
        if (obj.has("translation")) {
            float[] t = readVec3(obj.get("translation"), 0, 0, 0);
            tx = t[0];
            ty = t[1];
            tz = t[2];
        }
        if (obj.has("scale")) {
            float[] s = readVec3(obj.get("scale"), 1, 1, 1);
            sx = s[0];
            sy = s[1];
            sz = s[2];
        }

        return new Transform(rx, ry, rz, tx, ty, tz, sx, sy, sz);
    }

    private static float[] readVec3(JsonElement el, float dx, float dy, float dz) {
        if (!(el instanceof JsonArray arr) || arr.size() < 3) {
            return new float[] {dx, dy, dz};
        }
        return new float[] {
                arr.get(0).isJsonPrimitive() ? arr.get(0).getAsFloat() : dx,
                arr.get(1).isJsonPrimitive() ? arr.get(1).getAsFloat() : dy,
                arr.get(2).isJsonPrimitive() ? arr.get(2).getAsFloat() : dz
        };
    }
}
