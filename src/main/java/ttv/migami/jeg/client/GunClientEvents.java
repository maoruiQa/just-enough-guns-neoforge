package ttv.migami.jeg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.audio.StunRingingSound;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.client.audio.VehicleFireSoundInstance;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final float ADS_FOV_FACTOR = 0.35F;
    private static final float SCOPE_VIEWPORT_FOV = 20.0F;
    private static final ResourceLocation MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final ResourceLocation OVERHEAT_TEXTURE = Reference.id("textures/gui/timer/overheat.png");
    private static final ResourceLocation HOLD_TEXTURE = Reference.id("textures/gui/timer/hold.png");
    private static final int TIMER_BAR_WIDTH = 64;
    private static final int TIMER_BAR_HEIGHT = 6;
    private static final int OFFHAND_FULL_PROMPT_TICKS = 30;
    private static final Component MAGAZINE_UNLOAD_PROMPT = Component.translatable("jeg.magazine.unload.prompt");
    private static final Component OFFHAND_FULL_PROMPT = Component.translatable("jeg.magazine.offhand_full.prompt");
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean swapOffhandHeldLastTick;
    private static int offhandFullPromptTicks;
    private static long nextVisualShotTickMain;
    private static int rocketHoldTicks;
    private static boolean rocketHoldStartSent;
    private static boolean rocketShotSent;
    private static final java.util.Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final MuzzleFlashProfile DEFAULT_MUZZLE_FLASH = new MuzzleFlashProfile(0.8D, 0.0D, 3.96D, -4.785D);
    private static final java.util.Map<String, MuzzleFlashProfile> MUZZLE_FLASH_PROFILES = java.util.Map.ofEntries(
            java.util.Map.entry("abstract_gun", DEFAULT_MUZZLE_FLASH),
            java.util.Map.entry("assault_rifle", DEFAULT_MUZZLE_FLASH),
            java.util.Map.entry("finger_gun", new MuzzleFlashProfile(0.0D, 0.0D, 3.7D, -4.7D)),
            java.util.Map.entry("revolver", new MuzzleFlashProfile(0.8D, 0.0D, 4.695D, -2.785D)),
            java.util.Map.entry("waterpipe_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 3.89D, -7.89D)),
            java.util.Map.entry("custom_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -2.205D)),
            java.util.Map.entry("double_barrel_shotgun", new MuzzleFlashProfile(1.3D, 0.0D, 5.6D, -9.255D)),
            java.util.Map.entry("semi_auto_pistol", new MuzzleFlashProfile(0.8D, 0.0D, 5.645D, -2.2D)),
            java.util.Map.entry("semi_auto_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.43D, -7.305D)),
            java.util.Map.entry("pump_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 4.075D, -5.785D)),
            java.util.Map.entry("combat_pistol", new MuzzleFlashProfile(0.8D, 0.0D, 5.645D, -2.2D)),
            java.util.Map.entry("burst_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.34D, -5.865D)),
            java.util.Map.entry("combat_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.59D, -7.955D)),
            java.util.Map.entry("bolt_action_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.915D, -15.155D)),
            java.util.Map.entry("flare_gun", new MuzzleFlashProfile(0.8D, 0.0D, 4.695D, -2.04D)),
            java.util.Map.entry("blossom_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.4D, -9.7D)),
            java.util.Map.entry("holy_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 3.05D, -3.03D)),
            java.util.Map.entry("typhoonee", new MuzzleFlashProfile(0.8D, 0.0D, 2.5D, -3.03D)),
            java.util.Map.entry("repeating_shotgun", new MuzzleFlashProfile(0.8D, 0.0D, 4.645D, -10.635D)),
            java.util.Map.entry("infantry_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.495D, -9.655D)),
            java.util.Map.entry("service_rifle", new MuzzleFlashProfile(0.8D, 0.0D, 4.68D, -9.145D)),
            java.util.Map.entry("hollenfire_mk2", new MuzzleFlashProfile(0.8D, 0.0D, 4.68D, -7.645D)),
            java.util.Map.entry("soulhunter_mk2", new MuzzleFlashProfile(0.8D, 0.0D, 4.52D, -10.66D)),
            java.util.Map.entry("supersonic_shotgun", new MuzzleFlashProfile(1.0D, 0.0D, 4.315D, -5.855D)),
            java.util.Map.entry("hypersonic_cannon", new MuzzleFlashProfile(0.8D, 0.0D, 3.935D, -2.285D)),
            java.util.Map.entry("rocket_launcher", new MuzzleFlashProfile(1.3D, 0.0D, 4.695D, -8.015D)),
            java.util.Map.entry("grenade_launcher", new MuzzleFlashProfile(1.0D, 0.0D, 4.86D, -7.0D)),
            java.util.Map.entry("light_machine_gun", new MuzzleFlashProfile(0.8D, 0.0D, 4.88D, -10.0D)),
            java.util.Map.entry("flamethrower", new MuzzleFlashProfile(0.8D, 0.0D, 4.1D, -11.8D)),
            java.util.Map.entry("minigun", new MuzzleFlashProfile(1.0D, 0.0D, -1.1D, -13.0D)),
            java.util.Map.entry("phantom_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -2.205D))
    );
    private static StunRingingSound stunRingingSound;
    private static final java.util.Map<Integer, SoundInstance> VEHICLE_FIRE_SOUNDS = new java.util.HashMap<>();

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

        float ads = AimingHandler.get().getRenderAdsProgress();
        if (ads <= 0.0F) {
            return;
        }
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled()) {
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
        if (event.isUseItem() && GunItem.tryStartWaterCooling(player.level(), player, event.getHand())) {
            player.startUsingItem(event.getHand());
            return;
        }
        if (!(heldMain.getItem() instanceof GunItem)) {
            return;
        }

        if (event.isUseItem()) {
            // Right click is reserved for aiming (ADS), not firing.
            if (!held.is(Items.POTION)) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
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

        ItemStack held = player.getMainHandItem();
        Component promptText = null;
        int promptColor = 0xFFFFFFFF;
        if (held.getItem() instanceof MagazineItem magazine) {
            if (offhandFullPromptTicks > 0) {
                promptText = OFFHAND_FULL_PROMPT;
                promptColor = 0xFFFF5555;
            } else if (magazine.canShowUnloadPrompt(held, player.getOffhandItem())) {
                promptText = MAGAZINE_UNLOAD_PROMPT;
            } else {
                promptText = magazine.getLoadPromptMessage(held, player.getOffhandItem());
                if (promptText != null) {
                    promptColor = 0xFFFF5555;
                }
            }
        }

        if (held.getItem() instanceof GunItem gun && gun.usesOverheatMechanic()) {
            int heatPercent = gun.getOverheatPercent(held);
            if (heatPercent > 0 && Config.showTimersHud()) {
                renderOverheatBar(event.getGuiGraphics(), heatPercent);
            }
            if (gun.shouldShowWaterCoolingPrompt(held)) {
                promptText = Component.translatable("jeg.water_cooling.prompt");
            }
        }
        if (Config.showTimersHud() && GunItem.isHoldToFireWeapon(held) && rocketHoldTicks > 0 && !rocketShotSent) {
            renderHoldBar(event.getGuiGraphics(), rocketHoldTicks, GunItem.holdToFireTicks(held));
        }

        ItemStack offhand = player.getOffhandItem();
        if (Config.showTimersHud() && GunItem.canWaterCool(held) && GunItem.isCoolingWithWater(held)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(held));
        } else if (Config.showTimersHud() && GunItem.canWaterCool(offhand) && GunItem.isCoolingWithWater(offhand)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(offhand));
        }

        if (promptText != null) {
            renderCenteredOverlayPrompt(event.getGuiGraphics(), promptText, promptColor);
        }

        ClientHudRenderer.render(event.getGuiGraphics());
        if (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem) {
            ScopeOverlayRenderer.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            CrosshairHandler.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GunRecoilHandler.tick();
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
            VEHICLE_FIRE_SOUNDS.clear();
            CrosshairHandler.reset();
            return;
        }

        AimingHandler.get().tick(player);
        tickThrowableEffectAudio(player);
        tickVehicleFireAudio(player);
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
                    applyLocalVisualRecoil(gun);
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

        // R key reload (server-authoritative). Keep swap-hands reload as fallback/compat.
        if (!(player.getVehicle() instanceof ttv.migami.jeg.vehicle.entity.base.VehicleEntity) && KeyBindings.RELOAD.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.MAIN_HAND);
            } else if (heldOff.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.OFF_HAND);
            } else if (heldMain.getItem() instanceof MagazineItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelAfterEntities(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (Config.legacyBulletTrailEnabled()) {
            // Render trails once per frame globally so instant/hitscan shots remain visible.
            ttv.migami.jeg.client.render.BulletTrailRenderer.render(new PoseStack(), bufferSource, partialTick);
        }
        renderMuzzleFlashes(new PoseStack(), bufferSource, partialTick);
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
        if (minecraft.hasSingleplayerServer()) {
            flushRenderQueue();
        }
        // Clear bullet trails when logging out
        ttv.migami.jeg.client.render.BulletTrailRenderer.clear();
        MUZZLE_FLASHES.clear();
        attackHeldLastTick = false;
        aimingStateLastSent = false;
        swapOffhandHeldLastTick = false;
        offhandFullPromptTicks = 0;
        nextVisualShotTickMain = 0L;
        stunRingingSound = null;
        VEHICLE_FIRE_SOUNDS.clear();
        resetRocketHold(false);
        CrosshairHandler.reset();
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

    private static void applyLocalVisualRecoil(GunItem gun) {
        GunRecoilHandler.onShot(gun.getStats());
    }

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
            applyLocalVisualRecoil(gun);
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
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
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

    private record MuzzleFlashProfile(double size, double xOffset, double yOffset, double zOffset) {}

    private static void tickMuzzleFlashState() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            MUZZLE_FLASHES.clear();
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
        var cameraPos = camera.getPosition();

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

            Vec3 muzzlePos = computeMuzzlePosition(living, held, partialTick);
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
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(MUZZLE_FLASH_TEXTURE));

            consumer.addVertex(matrix, 0.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 0.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
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

    private static void renderOverheatBar(net.minecraft.client.gui.GuiGraphics guiGraphics, int heatPercent) {
        float ratio = Mth.clamp(heatPercent / 100.0F, 0.0F, 1.0F);
        int x = guiGraphics.guiWidth() / 2 - TIMER_BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() / 2 + 24;
        int filled = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(OVERHEAT_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(OVERHEAT_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, filled, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    private static void renderHoldBar(net.minecraft.client.gui.GuiGraphics guiGraphics, int holdTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return;
        }
        float ratio = Mth.clamp(holdTicks / (float) requiredTicks, 0.0F, 1.0F);
        int x = guiGraphics.guiWidth() / 2 - TIMER_BAR_WIDTH / 2;
        int y = guiGraphics.guiHeight() / 2 + 24;
        int filled = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(HOLD_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(HOLD_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, filled, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    private static void renderCenteredOverlayPrompt(net.minecraft.client.gui.GuiGraphics guiGraphics, Component text, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        int x = (guiGraphics.guiWidth() - minecraft.font.width(text)) / 2;
        int y = guiGraphics.guiHeight() - 88;
        guiGraphics.fill(x - 2, y - 2, x + minecraft.font.width(text) + 2, y + minecraft.font.lineHeight + 2, 0x66000000);
        guiGraphics.drawString(minecraft.font, text, x, y, color);
    }

    private static void renderWaterCoolingBar(net.minecraft.client.gui.GuiGraphics guiGraphics, int progressPercent) {
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
        VEHICLE_FIRE_SOUNDS.entrySet().removeIf(entry -> !minecraft.getSoundManager().isActive(entry.getValue()));
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof VehicleEntity vehicle)
                    || !vehicle.isWeaponFiring()
                    || vehicle.distanceToSqr(player) > 16384.0D) {
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

    private static void renderBlindOverlay(net.minecraft.client.gui.GuiGraphics guiGraphics, LocalPlayer player) {
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
