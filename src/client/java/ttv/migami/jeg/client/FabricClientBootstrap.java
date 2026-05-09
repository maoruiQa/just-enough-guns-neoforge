package ttv.migami.jeg.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.audio.StunRingingSound;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.GhoulRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerGeoRenderer;
import ttv.migami.jeg.client.render.entity.RaidEntityRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomGeoRenderer;
import ttv.migami.jeg.compat.ClientHooks;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.network.NetworkHandler;

public final class FabricClientBootstrap {
    private static final Gson GSON = new Gson();
    private static final Identifier MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final Identifier OVERHEAT_TEXTURE = Reference.id("textures/gui/timer/overheat.png");
    private static final Identifier HOLD_TEXTURE = Reference.id("textures/gui/timer/hold.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int TIMER_BAR_WIDTH = 64;
    private static final int TIMER_BAR_HEIGHT = 6;
    private static final int OFFHAND_FULL_PROMPT_TICKS = 30;
    private static final Component MAGAZINE_UNLOAD_PROMPT = Component.translatable("jeg.magazine.unload.prompt");
    private static final Component OFFHAND_FULL_PROMPT = Component.translatable("jeg.magazine.offhand_full.prompt");
    // Fabric 26.1 gun geo models point the muzzle toward negative local Z.
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
    private static boolean registered;
    private static String lastContextualPromptText = "";
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean swapOffhandHeldLastTick;
    private static int offhandFullPromptTicks;
    private static long nextVisualShotTickMain;
    private static int rocketHoldTicks;
    private static boolean rocketHoldStartSent;
    private static boolean rocketShotSent;
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

    private static final class MuzzleFlashState {
        private int ticksRemaining;
        private final float random;

        private MuzzleFlashState(int ticksRemaining, float random) {
            this.ticksRemaining = ticksRemaining;
            this.random = random;
        }
    }

    private record MuzzleFlashProfile(double size, double xOffset, double yOffset, double zOffset) {}

    private static final class FirstPersonGunPoseState {
        private final HumanoidArm arm;
        private final Matrix4f pose;

        private FirstPersonGunPoseState(HumanoidArm arm, Matrix4f pose) {
            this.arm = arm;
            this.pose = pose;
        }
    }

    private FabricClientBootstrap() {}

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;

        KeyBindings.init();
        ParticleFactoryRegistry.init();
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof GunItem) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            if (GunItem.tryStartWaterCooling(world, player, hand)) {
                player.startUsingItem(hand);
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        ClientHooks.setImpl(new ClientHooks.Impl() {
            @Override
            public void addDryFireRecoil(float amount) {
                GunRecoilHandler.addDryFire(amount);
            }

            @Override
            public void addShotRecoil(float amount) {
                GunRecoilHandler.addShot(amount);
            }

            @Override
            public void addBulletTrail(net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, int color, float size) {
                BulletTrailRenderer.addInstantTrail(start, end, color, size);
            }
        });

        ClientNetworkHandler.initClient();
        ClientSetup.registerClientExtensions(new RegisterClientExtensionsEvent());

        EntityRendererRegistry.register(ModEntities.GHOUL.get(), GhoulRenderer::new);
        EntityRendererRegistry.register(ModEntities.BULLET.get(), BulletRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.STUN_GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.SMOKE_GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.MOLOTOV_COCKTAIL.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.WATER_BOMB.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.RAID_ENTITY.get(), RaidEntityRenderer::new);

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            if (context.poseStack() == null || context.bufferSource() == null) {
                return;
            }
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
                BulletTrailRenderer.render(context.poseStack(), context.bufferSource(), partialTick);
            }
            renderMuzzleFlashes(context.poseStack(), context.bufferSource(), partialTick);
        });

        ClientTickEvents.START_CLIENT_TICK.register(FabricClientBootstrap::handleOffhandSwapOverride);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GunRecoilHandler.tick();
            if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
                BulletTrailRenderer.tick();
            }
            tickMuzzleFlashState();
            CrosshairHandler.tick();
            tickTransientPrompts(client);

            handleCombatInput(client);
            suppressSwingAnimation(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BulletTrailRenderer.clear();
            MUZZLE_FLASHES.clear();
            firstPersonGunPose = null;
            CrosshairHandler.reset();
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            swapOffhandHeldLastTick = false;
            offhandFullPromptTicks = 0;
            nextVisualShotTickMain = 0L;
            resetRocketHold(false);
            stunRingingSound = null;
            AimingHandler.get().reset();
            GunRecoilHandler.stopImmediate();
        });
    }

    private static void handleOffhandSwapOverride(Minecraft client) {
        LocalPlayer player = client.player;
        boolean swapDown = client.options.keySwapOffhand.isDown();
        if (player == null || client.level == null) {
            swapOffhandHeldLastTick = swapDown;
            return;
        }

        if (swapDown && !swapOffhandHeldLastTick && shouldInterceptOffhandSwap(player)) {
            while (client.options.keySwapOffhand.consumeClick()) {
                ClientNetworkHandler.sendUnloadMagazine();
            }
        }

        swapOffhandHeldLastTick = swapDown;
    }

    private static void handleCombatInput(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            nextVisualShotTickMain = 0L;
            resetRocketHold(false);
            CrosshairHandler.reset();
            return;
        }

        AimingHandler.get().tick(player);
        tickThrowableEffectAudio(player);
        boolean aiming = AimingHandler.get().isAiming();
        if (aiming != aimingStateLastSent) {
            aimingStateLastSent = aiming;
            ClientNetworkHandler.sendAiming(aiming);
        }

        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();

        if (heldMain.getItem() instanceof GunItem gun) {
            boolean attackDown = client.options.keyAttack.isDown();
            long nowTick = player.level().getGameTime();

            if (GunItem.isHoldToFireWeapon(heldMain)) {
                tickHoldToFire(player, heldMain, gun, attackDown, nowTick);
            } else if (attackDown) {
                resetRocketHold(true);
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    ClientNetworkHandler.sendShoot(InteractionHand.MAIN_HAND);
                }
                if (shouldApplyVisualRecoil(player, heldMain, gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(player, gun);
                    GunItem.recordClientShotSpread(player, gun.getStats());
                    CrosshairHandler.onGunFired();
                    forceExitScopedAdsAfterShot(gun);
                }
            } else if (attackHeldLastTick && !gun.isAutomatic() && GunItem.isTriggerLocked(heldMain)) {
                GunItem.clearTriggerLock(heldMain);
                ClientNetworkHandler.sendTriggerRelease(InteractionHand.MAIN_HAND);
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            } else {
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

        if (KeyBindings.RELOAD.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
            } else if (heldOff.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.OFF_HAND);
            } else if (heldMain.getItem() instanceof MagazineItem) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static boolean shouldInterceptOffhandSwap(LocalPlayer player) {
        ItemStack heldMain = player.getMainHandItem();
        if (!(heldMain.getItem() instanceof MagazineItem magazine)) {
            return false;
        }
        return magazine.canShowUnloadPrompt(heldMain, player.getOffhandItem());
    }

    private static void tickTransientPrompts(Minecraft client) {
        if (client.player == null || client.level == null) {
            offhandFullPromptTicks = 0;
            return;
        }
        if (offhandFullPromptTicks > 0) {
            offhandFullPromptTicks--;
        }
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

    private static void applyLocalVisualRecoil(LocalPlayer player, GunItem gun) {
        GunRecoilHandler.onShot(gun.getStats());
        AnimatedGunItem.suppressSprintAnimationBriefly();
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

    private static void suppressSwingAnimation(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.getItem() instanceof GunItem || off.getItem() instanceof GunItem) {
            player.attackAnim = 0.0F;
            player.oAttackAnim = 0.0F;
            player.swingTime = 0;
            player.swinging = false;
        }
    }

    public static void renderOverheatBar(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !Config.showTimersHud()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = screenWidth / 2 - TIMER_BAR_WIDTH / 2;
        int y = screenHeight / 2 + 24;
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
            int percent = gun.getOverheatPercent(held);
            if (percent > 0) {
                float ratio = Math.max(0.0F, Math.min(1.0F, percent / 100.0F));
                int fill = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERHEAT_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, OVERHEAT_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, fill, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
            }

            if (gun.shouldShowWaterCoolingPrompt(held)) {
                promptText = Component.translatable("jeg.water_cooling.prompt");
            }

            ItemStack offhand = player.getOffhandItem();
            ItemStack mainhand = player.getMainHandItem();
            ItemStack coolingStack = ItemStack.EMPTY;
            if (GunItem.canWaterCool(mainhand) && GunItem.isCoolingWithWater(mainhand)) {
                coolingStack = mainhand;
            } else if (GunItem.canWaterCool(offhand) && GunItem.isCoolingWithWater(offhand)) {
                coolingStack = offhand;
            }
            if (!coolingStack.isEmpty()) {
                int coolingY = y - 7;
                float coolingRatio = GunItem.getWaterCoolingProgressPercent(coolingStack) / 100.0F;
                int coolingFill = Math.max(1, Math.round(TIMER_BAR_WIDTH * coolingRatio));
                guiGraphics.fill(x, coolingY, x + TIMER_BAR_WIDTH, coolingY + TIMER_BAR_HEIGHT, 0x66000000);
                guiGraphics.fill(x, coolingY, x + coolingFill, coolingY + TIMER_BAR_HEIGHT, coolingColor(coolingRatio));
            }
        }
        if (GunItem.isHoldToFireWeapon(held) && rocketHoldTicks > 0 && !rocketShotSent) {
            renderHoldBar(guiGraphics, x, y, rocketHoldTicks, GunItem.holdToFireTicks(held));
        }

        if (promptText != null) {
            renderCenteredOverlayPrompt(guiGraphics, minecraft, promptText, y - 30, promptColor);
        } else {
            clearContextualOverlayPrompt(minecraft);
        }
    }

    private static void renderCenteredOverlayPrompt(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, Component text, int textY, int color) {
        int textX = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(text)) / 2;
        guiGraphics.fill(textX - 2, textY - 2, textX + minecraft.font.width(text) + 2, textY + minecraft.font.lineHeight + 2, 0x66000000);
        guiGraphics.text(minecraft.font, text, textX, textY, color, false);
        lastContextualPromptText = text.getString();
    }

    private static void clearContextualOverlayPrompt(Minecraft minecraft) {
        if (lastContextualPromptText.isEmpty()) {
            return;
        }
        lastContextualPromptText = "";
    }

    private static void renderHoldBar(GuiGraphicsExtractor guiGraphics, int x, int y, int holdTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return;
        }
        float ratio = Mth.clamp(holdTicks / (float) requiredTicks, 0.0F, 1.0F);
        int fill = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HOLD_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, HOLD_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, fill, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    public static void showOffhandFullPrompt() {
        offhandFullPromptTicks = OFFHAND_FULL_PROMPT_TICKS;
    }

    public static void renderThrowableEffectOverlay(GuiGraphicsExtractor guiGraphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        var effect = player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.BLINDED.get()));
        if (effect == null) {
            return;
        }

        float strength = Math.min(1.0F, effect.getDuration() / 40.0F);
        int alpha = Mth.clamp((int) (strength * 210.0F), 32, 210);
        int color = (alpha << 24) | 0x00FFFFFF;
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        guiGraphics.fill(0, 0, screenWidth, screenHeight, color);
    }

    private static int coolingColor(float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int red = Math.round(255.0F * (1.0F - clamped));
        int green = Math.round(255.0F * clamped);
        int blue = 48;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void tickThrowableEffectAudio(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean deafened = player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.DEAFENED.get()));
        if (!deafened) {
            stunRingingSound = null;
            return;
        }

        if (stunRingingSound == null || !minecraft.getSoundManager().isActive(stunRingingSound)) {
            stunRingingSound = new StunRingingSound();
            minecraft.getSoundManager().play(stunRingingSound);
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
        MUZZLE_FLASHES.put(entityId, new MuzzleFlashState(2, random));
    }

    private static void tickHoldToFire(LocalPlayer player, ItemStack stack, GunItem gun, boolean attackDown, long nowTick) {
        if (!attackDown) {
            if (rocketHoldStartSent) {
                ClientNetworkHandler.sendHoldFire(InteractionHand.MAIN_HAND, false);
            }
            if (attackHeldLastTick && GunItem.isTriggerLocked(stack)) {
                GunItem.clearTriggerLock(stack);
                ClientNetworkHandler.sendTriggerRelease(InteractionHand.MAIN_HAND);
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
            ClientNetworkHandler.sendHoldFire(InteractionHand.MAIN_HAND, true);
            rocketHoldStartSent = true;
        }

        rocketHoldTicks++;
        if (rocketHoldTicks < GunItem.holdToFireTicks(stack)) {
            return;
        }

        ClientNetworkHandler.sendShoot(InteractionHand.MAIN_HAND);
        rocketShotSent = true;
        if (shouldApplyVisualRecoil(player, stack, gun, false, nowTick)) {
            applyLocalVisualRecoil(player, gun);
            GunItem.recordClientShotSpread(player, gun.getStats());
            CrosshairHandler.onGunFired();
            forceExitScopedAdsAfterShot(gun);
        }
    }

    private static void resetRocketHold(boolean notifyServer) {
        if (notifyServer && rocketHoldStartSent) {
            ClientNetworkHandler.sendHoldFire(InteractionHand.MAIN_HAND, false);
        }
        rocketHoldTicks = 0;
        rocketHoldStartSent = false;
        rocketShotSent = false;
    }

    public static void captureFirstPersonGunPose(HumanoidArm arm, Matrix4f pose) {
        firstPersonGunPose = new FirstPersonGunPoseState(arm, new Matrix4f(pose));
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

    private static void renderMuzzleFlashes(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
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
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutout(MUZZLE_FLASH_TEXTURE));

            consumer.addVertex(matrix, 0.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 0.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 1.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 1.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(minU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(FULL_BRIGHT)
                    .setNormal(0.0F, 0.0F, 1.0F);
            consumer.addVertex(matrix, 0.0F, 1.0F, 0.0F)
                    .setColor(255, 255, 255, 255)
                    .setUv(maxU, 0.0F)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(FULL_BRIGHT)
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

    private static Vector3f firstPersonMuzzleAnchor(ItemStack held) {
        if (!(held.getItem() instanceof GunItem gun)) {
            return null;
        }
        Optional<Vector3f> cached = FIRST_PERSON_MUZZLE_ANCHORS.computeIfAbsent(
                gun.getStats().id(),
                FabricClientBootstrap::loadFirstPersonMuzzleAnchor
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
            if (definition == null) {
                continue;
            }
            definitions.put(definition.name(), definition);
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
}
