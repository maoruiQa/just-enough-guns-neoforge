package ttv.migami.jeg.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.audio.StunRingingSound;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.medal.MedalManager;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.client.audio.VehicleFireSoundInstance;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final Gson GSON = new Gson();
    private static final float ADS_FOV_FACTOR = 0.35F;
    private static final float SCOPE_VIEWPORT_FOV = 12.0F;
    private static final Identifier MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final Identifier OVERHEAT_TEXTURE = Reference.id("textures/gui/timer/overheat.png");
    private static final Identifier HOLD_TEXTURE = Reference.id("textures/gui/timer/hold.png");
    private static final int TIMER_BAR_WIDTH = 64;
    private static final int TIMER_BAR_HEIGHT = 6;
    private static final int OFFHAND_FULL_PROMPT_TICKS = 30;
    private static final Component MAGAZINE_UNLOAD_PROMPT = Component.translatable("jeg.magazine.unload.prompt");
    private static final Component OFFHAND_FULL_PROMPT = Component.translatable("jeg.magazine.offhand_full.prompt");
    private static final ByteBufferBuilder WORLD_EFFECT_BUFFER = new ByteBufferBuilder(262_144);
    private static final float MODEL_FRONT_CLUSTER_DEPTH = 0.45F;
    private static final float MODEL_FRONT_FACE_EPSILON = 0.02F;
    private static final List<String> FIRST_PERSON_MUZZLE_BONE_PRIORITY = List.of("front", "barrel", "gun_body", "bow_body");
    private static final Set<String> HIDDEN_FIRST_PERSON_BONES = Set.of(
            "left_arm",
            "right_arm",
            "fake_left_arm",
            "fake_right_arm",
            "attachment_bone",
            "railing",
            "silencer",
            "makeshift_stock",
            "light_stock",
            "tactical_stock",
            "weighted_stock",
            "light_grip",
            "vertical_grip",
            "angled_grip",
            "extended_mag",
            "extended_mag_2",
            "drum_mag",
            "drum_mag_2"
    );
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean swapOffhandHeldLastTick;
    private static int offhandFullPromptTicks;
    private static long nextVisualShotTickMain;
    private static int rocketHoldTicks;
    private static boolean rocketHoldStartSent;
    private static boolean rocketShotSent;
    private static ItemStack lastMainHandStackReference = ItemStack.EMPTY;
    private static int lastMainHandSlot = -1;
    private static final Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new ConcurrentHashMap<>();
    private static final MuzzleFlashProfile DEFAULT_MUZZLE_FLASH = new MuzzleFlashProfile(0.8D, 0.0D, 3.96D, -4.785D);
    private static final Map<String, MuzzleFlashProfile> MUZZLE_FLASH_PROFILES = Map.ofEntries(
            Map.entry("abstract_gun", DEFAULT_MUZZLE_FLASH),
            Map.entry("assault_rifle", DEFAULT_MUZZLE_FLASH),
            Map.entry("finger_gun", new MuzzleFlashProfile(0.0D, 0.0D, 3.7D, -4.7D)),
            Map.entry("revolver", new MuzzleFlashProfile(0.8D, 0.0D, 4.695D, -2.785D)),
            Map.entry("waterpipe_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 3.89D, -7.89D)),
            Map.entry("custom_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -2.205D)),
            Map.entry("double_barrel_shotgun", new MuzzleFlashProfile(1.3D, 0.0D, 5.6D, -9.255D)),
            Map.entry("semi_auto_pistol", new MuzzleFlashProfile(0.8D, 0.0D, 5.645D, -2.2D)),
            Map.entry("semi_auto_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.43D, -7.305D)),
            Map.entry("pump_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 4.075D, -5.785D)),
            Map.entry("combat_pistol", new MuzzleFlashProfile(0.8D, 0.0D, 5.645D, -2.2D)),
            Map.entry("burst_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.34D, -5.865D)),
            Map.entry("combat_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.59D, -7.955D)),
            Map.entry("bolt_action_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.915D, -15.155D)),
            Map.entry("flare_gun", new MuzzleFlashProfile(0.8D, 0.0D, 4.695D, -2.04D)),
            Map.entry("blossom_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.4D, -9.7D)),
            Map.entry("holy_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 3.05D, -3.03D)),
            Map.entry("typhoonee", new MuzzleFlashProfile(0.8D, 0.0D, 2.5D, -3.03D)),
            Map.entry("repeating_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 4.645D, -10.635D)),
            Map.entry("infantry_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.495D, -9.655D)),
            Map.entry("service_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.68D, -9.145D)),
            Map.entry("hollenfire_mk2", new MuzzleFlashProfile(0.8D, 0.0D, 4.68D, -7.645D)),
            Map.entry("soulhunter_mk2", new MuzzleFlashProfile(0.8D, 0.0D, 4.52D, -10.66D)),
            Map.entry("supersonic_shotgun", new MuzzleFlashProfile(1.0D, 0.0D, 4.315D, -5.855D)),
            Map.entry("hypersonic_cannon", new MuzzleFlashProfile(0.8D, 0.0D, 3.935D, -2.285D)),
            Map.entry("rocket_launcher", new MuzzleFlashProfile(1.3D, 0.0D, 4.695D, -8.015D)),
            Map.entry("grenade_launcher", new MuzzleFlashProfile(1.0D, 0.0D, 4.86D, -7.0D)),
            Map.entry("light_machine_gun", new MuzzleFlashProfile(0.8D, 0.0D, 4.88D, -10.0D)),
            Map.entry("flamethrower", new MuzzleFlashProfile(0.8D, 0.0D, 4.1D, -11.8D)),
            Map.entry("minigun", new MuzzleFlashProfile(1.0D, 0.0D, -1.1D, -13.0D)),
            Map.entry("phantom_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -2.205D))
    );
    private static final Map<Identifier, Optional<Vector3f>> FIRST_PERSON_MUZZLE_ANCHORS = new ConcurrentHashMap<>();
    private static FirstPersonGunPoseState firstPersonGunPose;
    private static StunRingingSound stunRingingSound;
    private static final Map<Integer, VehicleFireSoundInstance> VEHICLE_FIRE_SOUNDS = new HashMap<>();

    private GunClientEvents() {}

    private static final class MuzzleFlashState {
        private int ticksRemaining;
        private final float random;

        private MuzzleFlashState(int ticksRemaining, float random) {
            this.ticksRemaining = ticksRemaining;
            this.random = random;
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress();
        if (ads <= 0.0F) {
            return;
        }
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled(stack)) {
            float current = event.getNewFovModifier();
            float target = SCOPE_VIEWPORT_FOV / configuredFov();
            event.setNewFovModifier(Math.max(0.1F, Mth.lerp(ads, current, target)));
            return;
        }

        float fovFactor = ADS_FOV_FACTOR;
        float factor = 1.0F - fovFactor * ads;
        event.setNewFovModifier(Math.max(0.1F, event.getNewFovModifier() * factor));
    }

    private static float configuredFov() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null) {
            return 70.0F;
        }
        return Math.max(1.0F, minecraft.options.fov().get());
    }

    @SubscribeEvent
    public static void onCalculatePlayerTurn(CalculatePlayerTurnEvent event) {
        event.setMouseSensitivity(GunMouseSensitivityHandler.adjustFinalMouseSensitivity(event.getMouseSensitivity()));
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.getHand() == null) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getItemInHand(event.getHand());
        ItemStack heldMain = player.getMainHandItem();
        if (!(heldMain.getItem() instanceof GunItem)) {
            return;
        }

        if (event.isUseItem()) {
            // Right click is reserved for aiming (ADS), not firing or coolant use.
            event.setCanceled(true);
            event.setSwingHand(false);
        }

        if (event.isAttack()) {
            // Shooting is driven from client tick + C2S shoot packets.
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean swapDown = minecraft.options.keySwapOffhand.isDown();
        if (player == null || minecraft.level == null) {
            swapOffhandHeldLastTick = swapDown;
            return;
        }

        if (swapDown && !swapOffhandHeldLastTick && shouldInterceptOffhandSwap(player)) {
            while (minecraft.options.keySwapOffhand.consumeClick()) {
                NetworkHandler.sendUnloadMagazine();
            }
        }

        swapOffhandHeldLastTick = swapDown;
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!isCrosshairLayer(event)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        renderBlindOverlay(event.getGuiGraphics(), player);

        ItemStack mainhand = player.getMainHandItem();
        Component promptText = null;
        int promptColor = 0xFFFFFFFF;
        if (mainhand.getItem() instanceof MagazineItem magazine) {
            if (offhandFullPromptTicks > 0) {
                promptText = OFFHAND_FULL_PROMPT;
                promptColor = 0xFFFF5555;
            } else if (magazine.canShowUnloadPrompt(mainhand, player.getOffhandItem())) {
                promptText = MAGAZINE_UNLOAD_PROMPT;
            } else {
                promptText = magazine.getLoadPromptMessage(mainhand, player.getOffhandItem());
                if (promptText != null) {
                    promptColor = 0xFFFF5555;
                }
            }
        }
        if (mainhand.getItem() instanceof GunItem gun && gun.usesOverheatMechanic()) {
            int heatPercent = gun.getOverheatPercent(mainhand);
            if (heatPercent > 0 && Config.showTimersHud()) {
                renderOverheatBar(event.getGuiGraphics(), heatPercent);
            }
            if (gun.shouldShowWaterCoolingPrompt(mainhand)) {
                promptText = Component.translatable("jeg.water_cooling.prompt");
            }
        }
        if (Config.showTimersHud() && GunItem.isHoldToFireWeapon(mainhand) && rocketHoldTicks > 0 && !rocketShotSent) {
            renderHoldBar(event.getGuiGraphics(), rocketHoldTicks, GunItem.holdToFireTicks(mainhand));
        }

        ItemStack offhand = player.getOffhandItem();
        if (Config.showTimersHud() && GunItem.canWaterCool(mainhand) && GunItem.isCoolingWithWater(mainhand)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(mainhand));
        } else if (Config.showTimersHud() && GunItem.canWaterCool(offhand) && GunItem.isCoolingWithWater(offhand)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(offhand));
        }

        if (promptText != null) {
            renderCenteredOverlayPrompt(event.getGuiGraphics(), promptText, promptColor);
        }

        MedalManager.render(event.getGuiGraphics());
        ClientHudRenderer.render(event.getGuiGraphics());
        if (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem) {
            ScopeOverlayRenderer.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            CrosshairHandler.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            event.setCanceled(true);
        }
    }

    private static final class FirstPersonGunPoseState {
        private final HumanoidArm arm;
        private final Matrix4f pose;

        private FirstPersonGunPoseState(HumanoidArm arm, Matrix4f pose) {
            this.arm = arm;
            this.pose = pose;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GunRecoilHandler.tick();
        MedalManager.tick();
        if (Config.legacyBulletTrailEnabled()) {
            // Tick bullet trail renderer to age and remove old trails.
            ttv.migami.jeg.client.render.BulletTrailRenderer.tick();
        }
        tickMuzzleFlashState();
        CrosshairHandler.tick();
        if (offhandFullPromptTicks > 0) {
            offhandFullPromptTicks--;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            swapOffhandHeldLastTick = false;
            offhandFullPromptTicks = 0;
            nextVisualShotTickMain = 0L;
            resetRocketHold(false);
            clearImmediateGunSwitchState();
            AimingHandler.get().reset();
            CrosshairHandler.reset();
            return;
        }

        AimingHandler.get().tick(player);
        tickThrowableEffectAudio(player);
        tickVehicleFireAudio(player);
        tickImmediateGunSwitch(player);
        GunItem.tickClientVisualState(player);
        boolean aiming = AimingHandler.get().isAiming();
        if (aiming != aimingStateLastSent) {
            aimingStateLastSent = aiming;
            NetworkHandler.sendAiming(aiming);
        }

        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();
        if (heldMain.getItem() instanceof GunItem || heldOff.getItem() instanceof GunItem) {
            player.attackAnim = 0.0F;
            player.oAttackAnim = 0.0F;
            player.swingTime = 0;
            player.swinging = false;
        }

        if (heldMain.getItem() instanceof GunItem gun) {
            boolean attackDown = minecraft.options.keyAttack.isDown();
            long nowTick = player.level().getGameTime();
            if (GunItem.isHoldToFireWeapon(heldMain)) {
                tickHoldToFire(player, heldMain, gun, attackDown, nowTick);
            } else if (attackDown) {
                resetRocketHold(true);
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    NetworkHandler.sendShoot(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                if (shouldApplyVisualRecoil(player, heldMain, gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(player, heldMain, gun);
                    GunItem.recordClientShotSpread(player, gun.getStats());
                    CrosshairHandler.onGunFired();
                    forceExitScopedAdsAfterShot(gun);
                }
            } else if (attackHeldLastTick && !gun.isAutomatic() && GunItem.isTriggerLocked(heldMain)) {
                GunItem.clearTriggerLock(heldMain);
                NetworkHandler.sendTriggerRelease(net.minecraft.world.InteractionHand.MAIN_HAND);
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            } else if (!attackDown) {
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            }
            attackHeldLastTick = attackDown;
        } else {
            attackHeldLastTick = false;
            nextVisualShotTickMain = 0L;
            resetRocketHold(true);
            GunRecoilHandler.stopImmediate();
        }

        if (!(heldMain.getItem() instanceof GunItem) && heldMain.getItem() instanceof FlashlightAttachmentItem && minecraft.options.keyAttack.isDown()) {
            NetworkHandler.sendChargeFlashlight();
            minecraft.options.keyAttack.setDown(false);
        }

        // R key reload / coolant use (server-authoritative).
        if (!(player.getVehicle() instanceof VehicleEntity) && KeyBindings.RELOAD.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.MAIN_HAND);
                attackHeldLastTick = false;
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            } else if (heldOff.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.OFF_HAND);
            } else if (heldMain.getItem() instanceof MagazineItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
        if (!(player.getVehicle() instanceof VehicleEntity) && KeyBindings.ATTACHMENTS.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                NetworkHandler.sendOpenAttachments();
            }
        }
        if (!(player.getVehicle() instanceof VehicleEntity) && KeyBindings.MELEE.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem && canUseGunMelee(player, heldMain)) {
                GunItem.cancelReloadForImmediateAction(player, heldMain);
                if (heldMain.getItem() instanceof AnimatedGunItem) {
                    AnimatedGunItem.triggerClientMelee(minecraft.player);
                }
                NetworkHandler.sendMelee();
            }
        }

    }

    private static boolean canUseGunMelee(LocalPlayer player, ItemStack stack) {
        return !isMeleeBlockedGun(stack)
                && !GunItem.isDrawing(stack)
                && !player.getCooldowns().isOnCooldown(stack);
    }

    private static boolean isMeleeBlockedGun(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem gun)) {
            return false;
        }
        String path = gun.getStats().id().getPath();
        return "minigun".equals(path)
                || path.endsWith("bow")
                || path.endsWith("blowpipe");
    }

    @SubscribeEvent
    public static void onRenderLevelAfterOpaqueFeatures(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        boolean renderTrails = Config.legacyBulletTrailEnabled();
        boolean renderMuzzleFlashes = !MUZZLE_FLASHES.isEmpty();
        if (!renderTrails && !renderMuzzleFlashes) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        // NeoForge 26.1 does not provide the pass-local buffer source here, so use a dedicated
        // immediate buffer to mirror Fabric's world-render callback semantics.
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(WORLD_EFFECT_BUFFER);
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (renderTrails) {
            // Render trails once per frame globally so instant/hitscan shots remain visible.
            ttv.migami.jeg.client.render.BulletTrailRenderer.render(poseStack, bufferSource, partialTick);
        }
        if (renderMuzzleFlashes) {
            renderMuzzleFlashes(poseStack, bufferSource, partialTick);
        }
        bufferSource.endBatch();
    }

    private static boolean shouldInterceptOffhandSwap(LocalPlayer player) {
        ItemStack heldMain = player.getMainHandItem();
        if (!(heldMain.getItem() instanceof MagazineItem magazine)) {
            return false;
        }
        return magazine.canShowUnloadPrompt(heldMain, player.getOffhandItem());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        Minecraft minecraft = Minecraft.getInstance();
        flushRenderQueue();
        // Clear bullet trails when logging out
        ttv.migami.jeg.client.render.BulletTrailRenderer.clear();
        MUZZLE_FLASHES.clear();
        attackHeldLastTick = false;
        aimingStateLastSent = false;
        swapOffhandHeldLastTick = false;
        offhandFullPromptTicks = 0;
        nextVisualShotTickMain = 0L;
        AimingHandler.get().reset();
        resetRocketHold(false);
        clearImmediateGunSwitchState();
        GunRecoilHandler.stopImmediate();
        CrosshairHandler.reset();
        if (stunRingingSound != null) {
            minecraft.getSoundManager().stop(stunRingingSound);
            stunRingingSound = null;
        }
    }

    private static void flushRenderQueue() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        Runnable finish = () -> {
            if (org.lwjgl.opengl.GL.getCapabilities() != null) {
                GL11.glFinish();
            }
        };

        if (RenderSystem.isOnRenderThread()) {
            finish.run();
            return;
        }

        minecraft.submit(finish).join();
    }

    private static boolean shouldApplyVisualRecoil(LocalPlayer player, ItemStack stack, GunItem gun, boolean attackHeldLastTick, long nowTick) {
        if (!canPredictShot(player, stack, gun, attackHeldLastTick)) {
            return false;
        }
        int fireDelay = Math.max(1, gun.getStats().fireDelay());
        if (!gun.isAutomatic()) {
            nextVisualShotTickMain = nowTick + fireDelay;
            return true;
        }
        if (nowTick < nextVisualShotTickMain) {
            return false;
        }
        nextVisualShotTickMain = nowTick + fireDelay;
        return true;
    }

    private static void tickImmediateGunSwitch(LocalPlayer player) {
        int selectedSlot = player.getInventory().getSelectedSlot();
        ItemStack current = player.getMainHandItem();
        boolean changed = selectedSlot != lastMainHandSlot;

        if (changed && lastMainHandStackReference.getItem() instanceof AnimatedGunItem
                && lastMainHandStackReference != current
                && GunItem.isReloading(lastMainHandStackReference)) {
            GunItem.cancelClientReloadVisualForSwitch(player, lastMainHandStackReference, lastMainHandSlot);
        }

        if (changed
                && current.getItem() instanceof AnimatedGunItem
                && lastMainHandSlot >= 0
                && lastMainHandStackReference != current
                && !ItemStack.isSameItemSameComponents(lastMainHandStackReference, current)) {
            GunItem.startClientDrawAnimationForSwitch(player, current);
        }

        rememberMainHandStack(current, selectedSlot);
    }

    private static void rememberMainHandStack(ItemStack stack, int selectedSlot) {
        lastMainHandStackReference = stack;
        lastMainHandSlot = selectedSlot;
    }

    private static void clearImmediateGunSwitchState() {
        lastMainHandStackReference = ItemStack.EMPTY;
        lastMainHandSlot = -1;
    }

    private static void applyLocalVisualRecoil(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunRecoilHandler.onShot(gun.getStats(), GunAttachments.modifiers(stack));
        AnimatedGunItem.suppressSprintAnimationBriefly();
    }

    private record MuzzleFlashProfile(double size, double xOffset, double yOffset, double zOffset) {}

    private static void tickHoldToFire(LocalPlayer player, ItemStack stack, GunItem gun, boolean attackDown, long nowTick) {
        if (!attackDown) {
            if (rocketHoldStartSent) {
                NetworkHandler.sendHoldFire(net.minecraft.world.InteractionHand.MAIN_HAND, false);
            }
            if (attackHeldLastTick && GunItem.isTriggerLocked(stack)) {
                GunItem.clearTriggerLock(stack);
                NetworkHandler.sendTriggerRelease(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            resetRocketHold(false);
            nextVisualShotTickMain = 0L;
            GunRecoilHandler.stopImmediate();
            return;
        }

        if (rocketShotSent) {
            return;
        }

        if (!hasShootableAmmo(player, stack, gun)) {
            resetRocketHold(true);
            return;
        }

        if (!rocketHoldStartSent) {
            NetworkHandler.sendHoldFire(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            rocketHoldStartSent = true;
        }

        rocketHoldTicks++;
        if (rocketHoldTicks < GunItem.holdToFireTicks(stack)) {
            return;
        }

        NetworkHandler.sendShoot(net.minecraft.world.InteractionHand.MAIN_HAND);
        rocketShotSent = true;
        if (shouldApplyVisualRecoil(player, stack, gun, false, nowTick)) {
            applyLocalVisualRecoil(player, stack, gun);
            GunItem.recordClientShotSpread(player, gun.getStats());
            CrosshairHandler.onGunFired();
            forceExitScopedAdsAfterShot(gun);
        }
    }

    private static void resetRocketHold(boolean notifyServer) {
        if (notifyServer && rocketHoldStartSent) {
            NetworkHandler.sendHoldFire(net.minecraft.world.InteractionHand.MAIN_HAND, false);
        }
        rocketHoldTicks = 0;
        rocketHoldStartSent = false;
        rocketShotSent = false;
    }

    private static boolean canPredictShot(LocalPlayer player, ItemStack stack, GunItem gun, boolean attackHeldLastTick) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        if (!gun.isAutomatic() && (attackHeldLastTick || GunItem.isTriggerLocked(stack))) {
            return false;
        }
        if (gun.usesOverheatMechanic() && gun.getOverheatPercent(stack) >= 100) {
            return false;
        }
        return hasShootableAmmo(player, stack, gun);
    }

    private static boolean hasShootableAmmo(LocalPlayer player, ItemStack stack, GunItem gun) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (gun.isInventoryFedGun()) {
            return gun.countInventoryAmmo(player) > 0;
        }
        return gun.getMagazineAmmo(stack) > 0;
    }

    private static void forceExitScopedAdsAfterShot(GunItem gun) {
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled()) {
            AimingHandler.get().suppressUntilUseReleased();
        }
    }

    public static void showMuzzleFlash(int entityId, float random) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
        if (entity instanceof PhantomGunner) {
            return;
        }
        if (minecraft.player != null && minecraft.player.getId() == entityId) {
            boolean aiming = AimingHandler.get().isAiming();
            AnimatedGunItem.triggerClientShoot(minecraft.player, aiming);
            if (aiming) {
                return;
            }
        }
        if (usesNoMuzzleFlashGun(entity)) {
            return;
        }
        MUZZLE_FLASHES.put(entityId, new MuzzleFlashState(2, random));
    }

    private static boolean usesNoMuzzleFlashGun(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        ItemStack held = living.getMainHandItem();
        return held.getItem() instanceof GunItem gun && Reference.id("rocket_launcher").equals(gun.getStats().id());
    }

    public static void captureFirstPersonGunPose(HumanoidArm arm, Matrix4f pose) {
        firstPersonGunPose = new FirstPersonGunPoseState(arm, new Matrix4f(pose));
    }

    public static Vec3 getCurrentFirstPersonMuzzlePosition(LocalPlayer player, ItemStack held) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return null;
        }
        return computeStableFirstPersonMuzzlePosition(player, held, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    private static void tickMuzzleFlashState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            MUZZLE_FLASHES.clear();
            firstPersonGunPose = null;
            return;
        }
        MUZZLE_FLASHES.entrySet().removeIf(entry -> {
            MuzzleFlashState state = entry.getValue();
            state.ticksRemaining--;
            return state.ticksRemaining <= 0 || minecraft.level.getEntity(entry.getKey()) == null;
        });
    }

    private static void renderMuzzleFlashes(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || MUZZLE_FLASHES.isEmpty()) {
            return;
        }

        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.position();

        for (var entry : MUZZLE_FLASHES.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !entity.isAlive()) {
                continue;
            }

            ItemStack held = living.getMainHandItem();
            if (!(held.getItem() instanceof GunItem)) {
                continue;
            }
            if (usesNoMuzzleFlashGun(living)) {
                continue;
            }

            Vec3 muzzlePos = living == minecraft.player && minecraft.options.getCameraType().isFirstPerson()
                    ? getCurrentFirstPersonMuzzlePosition(minecraft.player, held)
                    : null;
            if (muzzlePos == null) {
                muzzlePos = computeMuzzlePosition(living, held, partialTick);
            }
            MuzzleFlashProfile flash = muzzleFlashProfile(held);
            poseStack.pushPose();
            poseStack.translate(muzzlePos.x - cameraPos.x, muzzlePos.y - cameraPos.y, muzzlePos.z - cameraPos.z);
            poseStack.mulPose(camera.rotation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(entry.getValue().random * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(entry.getValue().random >= 0.5F ? 180.0F : 0.0F));

            float size = (float) flash.size();
            poseStack.scale(size, size, 1.0F);
            poseStack.translate(-0.5F, -0.5F, 0.0F);

            float minU = held.isEnchanted() ? 0.5F : 0.0F;
            float maxU = held.isEnchanted() ? 1.0F : 0.5F;
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutout(MUZZLE_FLASH_TEXTURE));

            consumer.addVertex(matrix, 0.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(Brightness.FULL_BRIGHT.pack())
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(Brightness.FULL_BRIGHT.pack())
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(Brightness.FULL_BRIGHT.pack())
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 0.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(Brightness.FULL_BRIGHT.pack())
                    .setNormal(0.0F, 0.0F, 1.0F);

            poseStack.popPose();
        }
    }

    private static Vec3 computeMuzzlePosition(LivingEntity shooter, ItemStack held, float partialTick) {
        Vec3 eye = shooter.getEyePosition(partialTick);
        Vec3 look = shooter.getViewVector(partialTick);
        if (look.lengthSqr() < 1.0E-6D) {
            look = Vec3.ZERO;
        } else {
            look = look.normalize();
        }

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        MuzzleFlashProfile flash = muzzleFlashProfile(held);
        double forwardMul = 0.36D - flash.zOffset() * 0.0625D;
        double sideMul = flash.xOffset() * 0.0625D;
        double heightMul = (flash.yOffset() - 4.75D) * 0.0625D;

        if (shooter instanceof Player player && player.getMainArm() == HumanoidArm.LEFT) {
            sideMul *= -1.0D;
        }

        return eye.add(look.scale(forwardMul)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }

    private static MuzzleFlashProfile muzzleFlashProfile(ItemStack held) {
        if (held.getItem() instanceof GunItem gun) {
            return MUZZLE_FLASH_PROFILES.getOrDefault(gun.getStats().id().getPath(), DEFAULT_MUZZLE_FLASH);
        }
        return DEFAULT_MUZZLE_FLASH;
    }

    private static Vec3 computeFirstPersonMuzzlePosition(Minecraft minecraft, LocalPlayer player, ItemStack held) {
        FirstPersonGunPoseState poseState = firstPersonGunPose;
        if (poseState == null || poseState.arm != player.getMainArm()) {
            return null;
        }

        var camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        Vector3f localAnchor = firstPersonMuzzleAnchor(held);
        if (localAnchor == null) {
            return null;
        }

        Vector3f cameraSpace = poseState.pose.transformPosition(localAnchor, new Vector3f());
        cameraSpace.rotate(camera.rotation());
        return cameraPos.add(cameraSpace.x(), cameraSpace.y(), cameraSpace.z());
    }

    private static Vec3 computeStableFirstPersonMuzzlePosition(LocalPlayer player, ItemStack held, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        if (look.lengthSqr() < 1.0E-6D) {
            return eye;
        }
        look = look.normalize();

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = look.cross(up);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        double forwardMul = 0.52D;
        double sideMul = 0.10D;
        double heightMul = -0.10D;
        if (held.getItem() instanceof GunItem gun) {
            GunCategory category = GunCategory.fromStats(gun.getStats());
            if (category == GunCategory.RIFLE) {
                forwardMul = 0.60D;
            } else if (category == GunCategory.SHOTGUN) {
                forwardMul = 0.64D;
            } else if (category == GunCategory.SNIPER) {
                forwardMul = 0.70D;
            } else if (category == GunCategory.LMG) {
                forwardMul = 0.66D;
            } else if (category == GunCategory.HEAVY) {
                forwardMul = 0.76D;
                heightMul = -0.08D;
            }
        }

        if (player.getMainArm() == HumanoidArm.LEFT) {
            sideMul *= -1.0D;
        }

        return eye.add(look.scale(forwardMul)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }

    private static Vector3f firstPersonMuzzleAnchor(ItemStack held) {
        if (!(held.getItem() instanceof GunItem gun)) {
            return null;
        }

        Optional<Vector3f> cached = FIRST_PERSON_MUZZLE_ANCHORS.computeIfAbsent(
                gun.getStats().id(),
                GunClientEvents::loadFirstPersonMuzzleAnchor
        );
        return cached.map(Vector3f::new).orElse(null);
    }

    private static Optional<Vector3f> loadFirstPersonMuzzleAnchor(Identifier gunId) {
        Identifier modelId = Reference.id("geckolib/models/item/gun/" + gunId.getPath() + ".geo.json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(modelId);
            if (resource.isEmpty()) {
                return Optional.empty();
            }

            try (var in = resource.get().open();
                 var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("minecraft:geometry") || !root.get("minecraft:geometry").isJsonArray()) {
                    return Optional.empty();
                }

                JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
                if (geometries.isEmpty() || !geometries.get(0).isJsonObject()) {
                    return Optional.empty();
                }

                JsonObject geometry = geometries.get(0).getAsJsonObject();
                if (!geometry.has("bones") || !geometry.get("bones").isJsonArray()) {
                    return Optional.empty();
                }

                return resolvePreciseMuzzleAnchor(geometry.getAsJsonArray("bones"));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Vector3f> resolvePreciseMuzzleAnchor(JsonArray bones) {
        Map<String, BoneDefinition> definitions = parseBoneDefinitions(bones);
        if (definitions.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Matrix4f> transformCache = new HashMap<>();
        for (String candidateName : FIRST_PERSON_MUZZLE_BONE_PRIORITY) {
            BoneDefinition root = definitions.get(candidateName);
            if (root == null || shouldSkipFirstPersonBone(root.name())) {
                continue;
            }

            List<CubeFrontSample> samples = new ArrayList<>();
            collectVisibleCubeSamples(root, definitions, transformCache, samples);
            Vector3f anchor = resolveFrontClusterAnchor(samples);
            if (anchor != null) {
                return Optional.of(anchor);
            }
        }

        return Optional.empty();
    }

    private static Map<String, BoneDefinition> parseBoneDefinitions(JsonArray bones) {
        Map<String, BoneDefinition> definitions = new HashMap<>();
        for (JsonElement boneElement : bones) {
            if (!(boneElement instanceof JsonObject boneObject)) {
                continue;
            }

            BoneDefinition definition = parseBoneDefinition(boneObject);
            if (definition != null) {
                definitions.put(definition.name(), definition);
            }
        }

        for (BoneDefinition definition : definitions.values()) {
            if (!definition.parent().isEmpty()) {
                BoneDefinition parent = definitions.get(definition.parent());
                if (parent != null) {
                    parent.children().add(definition.name());
                }
            }
        }

        return definitions;
    }

    private static BoneDefinition parseBoneDefinition(JsonObject bone) {
        if (!bone.has("name") || !bone.get("name").isJsonPrimitive()) {
            return null;
        }

        String name = bone.get("name").getAsString();
        String parent = bone.has("parent") && bone.get("parent").isJsonPrimitive()
                ? bone.get("parent").getAsString()
                : "";
        Vector3f pivot = readVec3f(bone.get("pivot"));
        Vector3f rotation = readVec3f(bone.get("rotation"));
        List<CubeDefinition> cubes = new ArrayList<>();
        if (bone.has("cubes") && bone.get("cubes").isJsonArray()) {
            for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                if (!(cubeElement instanceof JsonObject cubeObject)) {
                    continue;
                }
                CubeDefinition cube = parseCubeDefinition(cubeObject);
                if (cube != null) {
                    cubes.add(cube);
                }
            }
        }

        return new BoneDefinition(name, parent, pivot, rotation, cubes, new ArrayList<>());
    }

    private static CubeDefinition parseCubeDefinition(JsonObject cube) {
        if (!cube.has("origin") || !cube.has("size")) {
            return null;
        }

        Vector3f origin = readVec3f(cube.get("origin"));
        Vector3f size = readVec3f(cube.get("size"));
        float inflate = cube.has("inflate") && cube.get("inflate").isJsonPrimitive()
                ? cube.get("inflate").getAsFloat()
                : 0.0F;
        if (inflate != 0.0F) {
            origin.sub(inflate, inflate, inflate);
            size.add(inflate * 2.0F, inflate * 2.0F, inflate * 2.0F);
        }

        Vector3f pivot = readVec3f(cube.get("pivot"));
        Vector3f rotation = readVec3f(cube.get("rotation"));
        return new CubeDefinition(origin, size, pivot, rotation);
    }

    private static void collectVisibleCubeSamples(
            BoneDefinition root,
            Map<String, BoneDefinition> definitions,
            Map<String, Matrix4f> transformCache,
            List<CubeFrontSample> samples
    ) {
        if (shouldSkipFirstPersonBone(root.name())) {
            return;
        }

        Matrix4f boneTransform = resolveBoneTransform(root, definitions, transformCache);
        for (CubeDefinition cube : root.cubes()) {
            CubeFrontSample sample = sampleCubeFront(cube, boneTransform);
            if (sample != null) {
                samples.add(sample);
            }
        }

        for (String childName : root.children()) {
            BoneDefinition child = definitions.get(childName);
            if (child != null) {
                collectVisibleCubeSamples(child, definitions, transformCache, samples);
            }
        }
    }

    private static Matrix4f resolveBoneTransform(
            BoneDefinition bone,
            Map<String, BoneDefinition> definitions,
            Map<String, Matrix4f> transformCache
    ) {
        Matrix4f cached = transformCache.get(bone.name());
        if (cached != null) {
            return new Matrix4f(cached);
        }

        Matrix4f transform = new Matrix4f();
        if (!bone.parent().isEmpty()) {
            BoneDefinition parent = definitions.get(bone.parent());
            if (parent != null) {
                transform.set(resolveBoneTransform(parent, definitions, transformCache));
            }
        }
        transform.mul(rotationMatrixAroundPivot(bone.pivot(), bone.rotation()));
        transformCache.put(bone.name(), new Matrix4f(transform));
        return transform;
    }

    private static Matrix4f rotationMatrixAroundPivot(Vector3f pivot, Vector3f rotationDeg) {
        return new Matrix4f()
                .translate(pivot.x(), pivot.y(), pivot.z())
                .rotateZ((float) Math.toRadians(rotationDeg.z()))
                .rotateY((float) Math.toRadians(rotationDeg.y()))
                .rotateX((float) Math.toRadians(rotationDeg.x()))
                .translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    private static CubeFrontSample sampleCubeFront(CubeDefinition cube, Matrix4f boneTransform) {
        Matrix4f cubeTransform = new Matrix4f(boneTransform);
        if (cube.hasRotation()) {
            cubeTransform.mul(rotationMatrixAroundPivot(cube.pivot(), cube.rotation()));
        }

        List<Vector3f> vertices = new ArrayList<>(8);
        float minZ = Float.POSITIVE_INFINITY;
        Vector3f origin = cube.origin();
        Vector3f size = cube.size();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    Vector3f vertex = new Vector3f(
                            origin.x() + (x == 0 ? 0.0F : size.x()),
                            origin.y() + (y == 0 ? 0.0F : size.y()),
                            origin.z() + (z == 0 ? 0.0F : size.z())
                    );
                    cubeTransform.transformPosition(vertex);
                    vertices.add(vertex);
                    minZ = Math.min(minZ, vertex.z());
                }
            }
        }

        Vector3f faceCenter = new Vector3f();
        int faceVertexCount = 0;
        for (Vector3f vertex : vertices) {
            if (vertex.z() <= minZ + MODEL_FRONT_FACE_EPSILON) {
                faceCenter.add(vertex);
                faceVertexCount++;
            }
        }
        if (faceVertexCount == 0) {
            return null;
        }

        faceCenter.div(faceVertexCount);
        float weight = Math.max(0.001F, Math.abs(size.x() * size.y()));
        return new CubeFrontSample(faceCenter, minZ, weight);
    }

    private static Vector3f resolveFrontClusterAnchor(List<CubeFrontSample> samples) {
        if (samples.isEmpty()) {
            return null;
        }

        float frontMostZ = Float.POSITIVE_INFINITY;
        for (CubeFrontSample sample : samples) {
            frontMostZ = Math.min(frontMostZ, sample.frontZ());
        }

        float clusterThreshold = frontMostZ + MODEL_FRONT_CLUSTER_DEPTH;
        Vector3f weightedCenter = new Vector3f();
        float totalWeight = 0.0F;
        for (CubeFrontSample sample : samples) {
            if (sample.frontZ() > clusterThreshold) {
                continue;
            }
            weightedCenter.fma(sample.weight(), sample.center());
            totalWeight += sample.weight();
        }

        if (totalWeight <= 0.0F) {
            return null;
        }

        weightedCenter.div(totalWeight);
        return new Vector3f(weightedCenter.x(), weightedCenter.y(), frontMostZ);
    }

    private static boolean shouldSkipFirstPersonBone(String boneName) {
        return HIDDEN_FIRST_PERSON_BONES.contains(boneName);
    }

    private static Vector3f readVec3f(JsonElement element) {
        if (!(element instanceof JsonArray array) || array.size() < 3) {
            return new Vector3f();
        }
        return new Vector3f(
                array.get(0).isJsonPrimitive() ? array.get(0).getAsFloat() : 0.0F,
                array.get(1).isJsonPrimitive() ? array.get(1).getAsFloat() : 0.0F,
                array.get(2).isJsonPrimitive() ? array.get(2).getAsFloat() : 0.0F
        );
    }

    private record BoneDefinition(
            String name,
            String parent,
            Vector3f pivot,
            Vector3f rotation,
            List<CubeDefinition> cubes,
            List<String> children
    ) {}

    private record CubeDefinition(Vector3f origin, Vector3f size, Vector3f pivot, Vector3f rotation) {
        private boolean hasRotation() {
            return Math.abs(rotation.x()) > 1.0E-4F
                    || Math.abs(rotation.y()) > 1.0E-4F
                    || Math.abs(rotation.z()) > 1.0E-4F;
        }
    }

    private record CubeFrontSample(Vector3f center, float frontZ, float weight) {}

    private static void renderOverheatBar(GuiGraphicsExtractor guiGraphics, int heatPercent) {
        float ratio = Mth.clamp(heatPercent / 100.0F, 0.0F, 1.0F);
        int x = guiGraphics.guiWidth() / 2 - TIMER_BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() / 2 + 24;
        int filled = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERHEAT_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERHEAT_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, filled, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    private static void renderHoldBar(GuiGraphicsExtractor guiGraphics, int holdTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return;
        }
        float ratio = Mth.clamp(holdTicks / (float) requiredTicks, 0.0F, 1.0F);
        int x = guiGraphics.guiWidth() / 2 - TIMER_BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() / 2 + 24;
        int filled = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HOLD_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HOLD_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, filled, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    private static void renderCenteredOverlayPrompt(GuiGraphicsExtractor guiGraphics, Component text, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        int x = (guiGraphics.guiWidth() - minecraft.font.width(text)) / 2;
        int y = guiGraphics.guiHeight() - 88;
        guiGraphics.fill(x - 2, y - 2, x + minecraft.font.width(text) + 2, y + minecraft.font.lineHeight + 2, 0x66000000);
        guiGraphics.text(minecraft.font, text, x, y, color);
    }

    private static void renderWaterCoolingBar(GuiGraphicsExtractor guiGraphics, int progressPercent) {
        float ratio = Mth.clamp(progressPercent / 100.0F, 0.0F, 1.0F);
        int x = guiGraphics.guiWidth() / 2 - TIMER_BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() / 2 + 17;
        int filled = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));

        guiGraphics.fill(x, y, x + TIMER_BAR_WIDTH, y + TIMER_BAR_HEIGHT, 0x66000000);
        guiGraphics.fill(x, y, x + filled, y + TIMER_BAR_HEIGHT, coolingColor(ratio));
    }

    private static int coolingColor(float ratio) {
        float clamped = Mth.clamp(ratio, 0.0F, 1.0F);
        int red = Mth.floor(255.0F * (1.0F - clamped));
        int green = Mth.floor(255.0F * clamped);
        int blue = 32;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void tickThrowableEffectAudio(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean deafened = player.hasEffect(ModEffects.DEAFENED);
        if (!deafened) {
            stunRingingSound = null;
            return;
        }

        if (stunRingingSound == null || !minecraft.getSoundManager().isActive(stunRingingSound)) {
            stunRingingSound = new StunRingingSound();
            minecraft.getSoundManager().play(stunRingingSound);
        }
    }

    private static void tickVehicleFireAudio(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            VEHICLE_FIRE_SOUNDS.clear();
            return;
        }
        VEHICLE_FIRE_SOUNDS.entrySet().removeIf(entry -> {
            var entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof VehicleEntity vehicle)
                    || !vehicle.isWeaponFiring()
                    || vehicle.distanceToSqr(player) > 16384.0D) {
                minecraft.getSoundManager().stop(entry.getValue());
                return true;
            }
            var sound = vehicle.activeVehicleFireSound();
            if (sound == null || !entry.getValue().matches(sound)) {
                minecraft.getSoundManager().stop(entry.getValue());
                return true;
            }
            return false;
        });
        for (var entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof VehicleEntity vehicle) || !vehicle.isWeaponFiring() || vehicle.distanceToSqr(player) > 16384.0D) {
                continue;
            }
            if (VEHICLE_FIRE_SOUNDS.containsKey(vehicle.getId())) {
                continue;
            }
            var sound = vehicle.activeVehicleFireSound();
            if (sound == null) {
                continue;
            }
            VehicleFireSoundInstance instance = new VehicleFireSoundInstance(vehicle, sound);
            VEHICLE_FIRE_SOUNDS.put(vehicle.getId(), instance);
            minecraft.getSoundManager().play(instance);
        }
    }

    private static void renderBlindOverlay(GuiGraphicsExtractor guiGraphics, LocalPlayer player) {
        var effect = player.getEffect(ModEffects.BLINDED);
        if (effect == null) {
            return;
        }

        float strength = Math.min(1.0F, effect.getDuration() / 40.0F);
        int alpha = Mth.clamp((int) (strength * 210.0F), 32, 210);
        int color = (alpha << 24) | 0x00FFFFFF;
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
    }

    private static boolean isCrosshairLayer(RenderGuiLayerEvent event) {
        return event.getName() != null && "crosshair".equals(event.getName().getPath());
    }

    public static void showOffhandFullPrompt() {
        offhandFullPromptTicks = OFFHAND_FULL_PROMPT_TICKS;
    }

}
