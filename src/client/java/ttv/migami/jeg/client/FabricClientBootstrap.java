package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.audio.StunRingingSound;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.medal.MedalManager;
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
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;
import ttv.migami.jeg.vehicle.client.audio.VehicleFireSoundInstance;
import ttv.migami.jeg.vehicle.client.render.A10Renderer;
import ttv.migami.jeg.vehicle.client.render.Ah6Renderer;
import ttv.migami.jeg.vehicle.client.render.Bmp2Renderer;
import ttv.migami.jeg.vehicle.client.render.Hpj11Renderer;
import ttv.migami.jeg.vehicle.client.render.LaserTowerRenderer;
import ttv.migami.jeg.vehicle.client.render.Lav150Renderer;
import ttv.migami.jeg.vehicle.client.render.Mi28Renderer;
import ttv.migami.jeg.vehicle.client.render.SpeedboatRenderer;
import ttv.migami.jeg.vehicle.client.render.Tom6Renderer;
import ttv.migami.jeg.vehicle.client.render.TruckRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleDecoyRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleGeoRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleMissileRenderer;
import ttv.migami.jeg.vehicle.client.render.WaveforceTowerRenderer;
import ttv.migami.jeg.vehicle.client.render.block.VehicleAssemblingTableBlockEntityRenderer;
import ttv.migami.jeg.client.screen.AttachmentScreen;
import ttv.migami.jeg.client.screen.MagazineLoaderScreen;
import ttv.migami.jeg.vehicle.client.screen.VehicleAssemblingScreen;
import ttv.migami.jeg.vehicle.client.screen.VehicleChargingStationScreen;
import ttv.migami.jeg.vehicle.client.screen.VehicleScreen;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class FabricClientBootstrap {
    private static final ResourceLocation MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final double THIRD_PERSON_MUZZLE_FORWARD_OFFSET = 1.35D;
    private static final ResourceLocation OVERHEAT_TEXTURE = Reference.id("textures/gui/timer/overheat.png");
    private static final ResourceLocation HOLD_TEXTURE = Reference.id("textures/gui/timer/hold.png");
    private static final int TIMER_BAR_WIDTH = 64;
    private static final int TIMER_BAR_HEIGHT = 6;
    private static final int OFFHAND_FULL_PROMPT_TICKS = 30;
    private static final Component MAGAZINE_UNLOAD_PROMPT = Component.translatable("jeg.magazine.unload.prompt");
    private static final Component OFFHAND_FULL_PROMPT = Component.translatable("jeg.magazine.offhand_full.prompt");
    private static boolean registered;
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean swapOffhandHeldLastTick;
    private static boolean meleeHeldLastTick;
    private static int offhandFullPromptTicks;
    private static long nextVisualShotTickMain;
    private static int rocketHoldTicks;
    private static boolean rocketHoldStartSent;
    private static boolean rocketShotSent;
    private static String lastContextualPromptText = "";
    private static ItemStack lastMainHandStackReference = ItemStack.EMPTY;
    private static int lastMainHandSlot = -1;
    private static final Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new ConcurrentHashMap<>();
    private static final Map<Integer, VehicleFireSoundInstance> VEHICLE_FIRE_SOUNDS = new HashMap<>();
    private static final Set<String> ALT_MUZZLE_FLASH_IDS = Set.of(
            "subsonic_rifle",
            "flamethrower",
            "supersonic_shotgun",
            "hypersonic_cannon",
            "soulhunter_mk2",
            "blossom_rifle",
            "holy_shotgun"
    );
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
            Map.entry("atlantean_spear", new MuzzleFlashProfile(0.8D, 0.0D, 2.05D, -4.03D)),
            Map.entry("typhoonee", new MuzzleFlashProfile(0.8D, 0.0D, 2.5D, -3.03D)),
            Map.entry("bubble_cannon", new MuzzleFlashProfile(0.8D, 0.0D, 2.5D, -3.03D)),
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
            Map.entry("vindicator_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -5.0D)),
            Map.entry("fire_sweeper", new MuzzleFlashProfile(0.8D, 0.0D, 4.645D, -10.635D)),
            Map.entry("phantom_smg", new MuzzleFlashProfile(0.8D, 0.0D, 4.45D, -2.205D))
    );
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

    private FabricClientBootstrap() {}

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;

        ModelLoadingPlugin.register(new FabricModelRegistration());
        KeyBindings.init();

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof GunItem) {
                return net.minecraft.world.InteractionResultHolder.fail(held);
            }
            if (player.getMainHandItem().getItem() instanceof GunItem) {
                return net.minecraft.world.InteractionResultHolder.pass(held);
            }
            if (GunItem.tryStartWaterCooling(world, player, hand)) {
                player.startUsingItem(hand);
                return net.minecraft.world.InteractionResultHolder.consume(held);
            }
            return net.minecraft.world.InteractionResultHolder.pass(held);
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getMainHandItem().getItem() instanceof GunItem) {
                return net.minecraft.world.InteractionResult.FAIL;
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
            public void addBulletTrail(Vec3 start, Vec3 end, int color, float size) {
                BulletTrailRenderer.addInstantTrail(start, end, color, size);
            }
        });
        ClientNetworkHandler.initClient();
        ParticleFactoryRegistry.init();
        registerClientExtensions(new RegisterClientExtensionsEvent());

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
        EntityRendererRegistry.register(ModEntities.TEST_WHEEL_VEHICLE.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.LIGHT_COMBAT_VEHICLE.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_HELICOPTER.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_BOAT.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_ARTILLERY.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_AIRCRAFT.get(), VehicleGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRUCK.get(), TruckRenderer::new);
        EntityRendererRegistry.register(ModEntities.LAV150.get(), Lav150Renderer::new);
        EntityRendererRegistry.register(ModEntities.SPEEDBOAT.get(), SpeedboatRenderer::new);
        EntityRendererRegistry.register(ModEntities.AH6.get(), Ah6Renderer::new);
        EntityRendererRegistry.register(ModEntities.A10.get(), A10Renderer::new);
        EntityRendererRegistry.register(ModEntities.BMP2.get(), Bmp2Renderer::new);
        EntityRendererRegistry.register(ModEntities.MI28.get(), Mi28Renderer::new);
        EntityRendererRegistry.register(ModEntities.TOM6.get(), Tom6Renderer::new);
        EntityRendererRegistry.register(ModEntities.LASER_TOWER.get(), LaserTowerRenderer::new);
        EntityRendererRegistry.register(ModEntities.HPJ11.get(), Hpj11Renderer::new);
        EntityRendererRegistry.register(ModEntities.WAVEFORCE_TOWER.get(), WaveforceTowerRenderer::new);
        EntityRendererRegistry.register(ModEntities.VEHICLE_DECOY.get(), VehicleDecoyRenderer::new);
        EntityRendererRegistry.register(ModEntities.VEHICLE_MISSILE.get(), VehicleMissileRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), context -> new VehicleAssemblingTableBlockEntityRenderer());
        MenuScreens.register(ModMenuTypes.ATTACHMENT_MENU.get(), AttachmentScreen::new);
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU.get(), VehicleScreen::new);
        MenuScreens.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), VehicleAssemblingScreen::new);
        MenuScreens.register(ModMenuTypes.VEHICLE_CHARGING_STATION_MENU.get(), VehicleChargingStationScreen::new);
        MenuScreens.register(ModMenuTypes.MAGAZINE_LOADER_MENU.get(), MagazineLoaderScreen::new);

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
                player != null && player.getMainHandItem().getItem() instanceof GunItem);

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            PoseStack matrixStack = context.matrixStack();
            if (matrixStack == null) {
                return;
            }
            if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
                BulletTrailRenderer.render(matrixStack, context.consumers(), partialTick);
            }
            renderMuzzleFlashes(matrixStack, context.consumers(), partialTick);
        });

        ClientTickEvents.START_CLIENT_TICK.register(FabricClientBootstrap::handleOffhandSwapOverride);
        ClientTickEvents.END_CLIENT_TICK.register(VehicleInputHandler::onClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(FabricClientBootstrap::onClientTick);
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register((ignoredScreen, key, scancode, modifiers) ->
                        VehicleInputHandler.onScreenKeyPressed(key, scancode)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BulletTrailRenderer.clear();
            MUZZLE_FLASHES.clear();
            AimingHandler.get().reset();
            CrosshairHandler.reset();
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            swapOffhandHeldLastTick = false;
            meleeHeldLastTick = false;
            offhandFullPromptTicks = 0;
            nextVisualShotTickMain = 0L;
            resetRocketHold(false);
            clearImmediateGunSwitchState();
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

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (var holder : ModItems.GUNS.values()) {
            event.registerItem(new GunItemClientExtensions(holder.get()), holder.get());
        }
    }

    private static void onClientTick(Minecraft client) {
        GunRecoilHandler.tick();
        MedalManager.tick();
        if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
            BulletTrailRenderer.tick();
        }
        tickMuzzleFlashState();
        CrosshairHandler.tick();
        tickTransientPrompts(client);

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            swapOffhandHeldLastTick = false;
            meleeHeldLastTick = false;
            offhandFullPromptTicks = 0;
            nextVisualShotTickMain = 0L;
            resetRocketHold(false);
            clearImmediateGunSwitchState();
            stunRingingSound = null;
            MUZZLE_FLASHES.clear();
            VEHICLE_FIRE_SOUNDS.clear();
            AimingHandler.get().reset();
            CrosshairHandler.reset();
            GunRecoilHandler.stopImmediate();
            return;
        }

        tickImmediateGunSwitch(player);
        AimingHandler.get().tick(player);
        tickThrowableEffectAudio(player);
        tickVehicleFireAudio(player);
        boolean aiming = AimingHandler.get().isAiming();
        if (aiming != aimingStateLastSent) {
            aimingStateLastSent = aiming;
            ClientNetworkHandler.sendAiming(aiming);
        }

        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();

        boolean drawLocked = GunItem.isDrawOperationLocked(heldMain);
        while (consumeUnlockedClick(KeyBindings.ATTACHMENTS, drawLocked)) {
            if (!(player.getVehicle() instanceof VehicleEntity) && heldMain.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendOpenAttachments();
            }
        }

        if (heldMain.getItem() instanceof GunItem gun) {
            boolean attackDown = client.options.keyAttack.isDown();
            long nowTick = player.level().getGameTime();

            if (drawLocked) {
                resetRocketHold(true);
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            } else if (GunItem.isHoldToFireWeapon(heldMain)) {
                tickHoldToFire(player, heldMain, gun, attackDown, nowTick);
            } else if (attackDown) {
                resetRocketHold(true);
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    ClientNetworkHandler.sendShoot(InteractionHand.MAIN_HAND);
                }
                if (shouldApplyVisualRecoil(player, heldMain, gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(player, heldMain, gun);
                    GunItem.recordClientShotSpread(player, gun.getStats());
                    CrosshairHandler.onGunFired();
                    forceExitScopedAdsAfterShot(heldMain, gun);
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

        if (!(heldMain.getItem() instanceof GunItem) && heldMain.getItem() instanceof FlashlightAttachmentItem && client.options.keyAttack.isDown()) {
            ClientNetworkHandler.sendChargeFlashlight();
            client.options.keyAttack.setDown(false);
        }

        boolean meleeDown = KeyBindings.MELEE.isDown()
                || InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_V);
        boolean meleePressed = KeyBindings.MELEE.consumeClick() || (meleeDown && !meleeHeldLastTick);
        if (!(player.getVehicle() instanceof VehicleEntity) && meleePressed
                && heldMain.getItem() instanceof GunItem && canUseGunMelee(player, heldMain)) {
            GunItem.cancelReloadForImmediateAction(player, heldMain);
            AnimatedGunItem.triggerClientMelee(player);
            ClientNetworkHandler.sendMelee();
        }
        if (drawLocked && !meleeDown) {
            drainClicks(KeyBindings.MELEE);
        }
        meleeHeldLastTick = meleeDown;

        if (!(player.getVehicle() instanceof VehicleEntity) && consumeUnlockedClick(KeyBindings.INSPECT, drawLocked)
                && heldMain.getItem() instanceof GunItem && !GunItem.isDrawOperationLocked(heldMain)) {
            if (heldMain.getItem() instanceof AnimatedGunItem) {
                AnimatedGunItem.triggerClientInspect(player);
            }
            ClientNetworkHandler.sendInspect();
        }

        if (!(player.getVehicle() instanceof VehicleEntity) && consumeUnlockedClick(KeyBindings.RELOAD, drawLocked)) {
            if (heldMain.getItem() instanceof GunItem && !GunItem.isDrawOperationLocked(heldMain)) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
            } else if (heldOff.getItem() instanceof GunItem && !GunItem.isDrawOperationLocked(heldOff)) {
                ClientNetworkHandler.sendReload(InteractionHand.OFF_HAND);
            } else if (heldMain.getItem() instanceof MagazineItem) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
            }
        }

        suppressSwingAnimation(player, heldMain, heldOff);
    }

    private static boolean canUseGunMelee(LocalPlayer player, ItemStack stack) {
        return !isMeleeBlockedGun(stack)
                && !GunItem.isDrawOperationLocked(stack)
                && !player.getCooldowns().isOnCooldown(stack.getItem());
    }

    private static boolean consumeUnlockedClick(net.minecraft.client.KeyMapping key, boolean locked) {
        if (!locked) {
            return key.consumeClick();
        }
        if (!key.isDown()) {
            drainClicks(key);
        }
        return false;
    }

    private static void drainClicks(net.minecraft.client.KeyMapping key) {
        while (key.consumeClick()) {
        }
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

    private static boolean shouldInterceptOffhandSwap(LocalPlayer player) {
        ItemStack heldMain = player.getMainHandItem();
        if (!(heldMain.getItem() instanceof MagazineItem magazine)) {
            return false;
        }
        return magazine.canShowUnloadPrompt(heldMain, player.getOffhandItem());
    }

    private static void tickImmediateGunSwitch(LocalPlayer player) {
        int selectedSlot = player.getInventory().selected;
        ItemStack current = player.getMainHandItem();
        boolean changed = selectedSlot != lastMainHandSlot;

        if (changed && lastMainHandStackReference.getItem() instanceof AnimatedGunItem
                && lastMainHandStackReference != current
                && GunItem.isReloading(lastMainHandStackReference)) {
            GunItem.cancelClientReloadVisualForSwitch(player, lastMainHandStackReference, lastMainHandSlot);
        }

        if (changed
                && current.getItem() instanceof AnimatedGunItem
                && lastMainHandSlot >= 0) {
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

    private static void tickTransientPrompts(Minecraft client) {
        if (client.player == null || client.level == null) {
            offhandFullPromptTicks = 0;
            return;
        }
        if (offhandFullPromptTicks > 0) {
            offhandFullPromptTicks--;
        }
    }

    private static boolean shouldApplyVisualRecoil(LocalPlayer player, ItemStack stack, GunItem gun, boolean wasHeldLastTick, long nowTick) {
        if (!canPredictShot(player, stack, gun, wasHeldLastTick)) {
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

    private static void applyLocalVisualRecoil(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunRecoilHandler.onShot(gun.getStats(), GunAttachments.modifiers(stack));
    }

    private static boolean canPredictShot(LocalPlayer player, ItemStack stack, GunItem gun, boolean wasHeldLastTick) {
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return false;
        }
        if (!gun.isAutomatic() && (wasHeldLastTick || GunItem.isTriggerLocked(stack))) {
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

    private static void forceExitScopedAdsAfterShot(ItemStack stack, GunItem gun) {
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.hasTelescopicSight(stack)) {
            AimingHandler.get().suppressUntilUseReleased();
        }
    }

    private static void suppressSwingAnimation(LocalPlayer player, ItemStack main, ItemStack off) {
        if (main.getItem() instanceof GunItem || off.getItem() instanceof GunItem) {
            player.attackAnim = 0.0F;
            player.oAttackAnim = 0.0F;
            player.swingTime = 0;
            player.swinging = false;
        }
    }

    public static void renderOverheatBar(GuiGraphics guiGraphics) {
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
                guiGraphics.blit(OVERHEAT_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
                guiGraphics.blit(OVERHEAT_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, fill, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
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

    private static void renderCenteredOverlayPrompt(GuiGraphics guiGraphics, Minecraft minecraft, Component text, int textY, int color) {
        int textX = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(text)) / 2;
        guiGraphics.fill(textX - 2, textY - 2, textX + minecraft.font.width(text) + 2, textY + minecraft.font.lineHeight + 2, 0x66000000);
        guiGraphics.drawString(minecraft.font, text, textX, textY, color);
        lastContextualPromptText = text.getString();
    }

    private static void clearContextualOverlayPrompt(Minecraft minecraft) {
        if (lastContextualPromptText.isEmpty()) {
            return;
        }
        lastContextualPromptText = "";
    }

    private static void renderHoldBar(GuiGraphics guiGraphics, int x, int y, int holdTicks, int requiredTicks) {
        if (requiredTicks <= 0) {
            return;
        }
        float ratio = Mth.clamp(holdTicks / (float) requiredTicks, 0.0F, 1.0F);
        int fill = Math.max(1, Math.round(TIMER_BAR_WIDTH * ratio));
        guiGraphics.blit(HOLD_TEXTURE, x, y, 0.0F, 0.0F, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
        guiGraphics.blit(HOLD_TEXTURE, x, y, 0.0F, TIMER_BAR_HEIGHT, fill, TIMER_BAR_HEIGHT, TIMER_BAR_WIDTH, TIMER_BAR_HEIGHT * 2);
    }

    public static void showOffhandFullPrompt() {
        offhandFullPromptTicks = OFFHAND_FULL_PROMPT_TICKS;
    }

    public static void renderThrowableEffectOverlay(GuiGraphics guiGraphics) {
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
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), color);
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

    private static void tickVehicleFireAudio(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            VEHICLE_FIRE_SOUNDS.clear();
            return;
        }
        VEHICLE_FIRE_SOUNDS.entrySet().removeIf(entry -> {
            Entity entity = minecraft.level.getEntity(entry.getKey());
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
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof VehicleEntity vehicle)
                    || !vehicle.isWeaponFiring()
                    || vehicle.distanceToSqr(player) > 16384.0D
                    || VEHICLE_FIRE_SOUNDS.containsKey(vehicle.getId())) {
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

    public static void showMuzzleFlash(int entityId, float random) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level != null ? minecraft.level.getEntity(entityId) : null;
        if (entity instanceof PhantomGunner) {
            return;
        }
        if (minecraft.player != null && minecraft.player.getId() == entityId) {
            boolean aiming = AimingHandler.get().isAiming();
            AnimatedGunItem.triggerClientShoot(minecraft.player, aiming);
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
        if (GunItem.isDrawOperationLocked(stack)) {
            resetRocketHold(true);
            nextVisualShotTickMain = 0L;
            GunRecoilHandler.stopImmediate();
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
            applyLocalVisualRecoil(player, stack, gun);
            GunItem.recordClientShotSpread(player, gun.getStats());
            CrosshairHandler.onGunFired();
            forceExitScopedAdsAfterShot(stack, gun);
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

    private static void renderMuzzleFlashes(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || MUZZLE_FLASHES.isEmpty() || bufferSource == null) {
            return;
        }

        var camera = minecraft.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();

        for (var entry : MUZZLE_FLASHES.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !entity.isAlive()) {
                continue;
            }
            if (entity == minecraft.player && minecraft.options.getCameraType().isFirstPerson()) {
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

            renderMuzzleFlashQuad(poseStack, bufferSource, held, flash);

            poseStack.popPose();
        }
    }

    public static void renderFirstPersonMuzzleFlashRelativeToBone(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ItemStack held,
            HumanoidArm arm,
            double bonePivotX,
            double bonePivotY,
            double bonePivotZ
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(held.getItem() instanceof GunItem)) {
            return;
        }

        MuzzleFlashState state = MUZZLE_FLASHES.get(minecraft.player.getId());
        if (state == null) {
            return;
        }

        MuzzleFlashProfile flash = muzzleFlashProfile(held);
        double xOffset = flash.xOffset() - bonePivotX;
        if (arm == HumanoidArm.LEFT) {
            xOffset *= -1.0D;
        }

        poseStack.pushPose();
        poseStack.translate(xOffset * 0.0625D, (flash.yOffset() - bonePivotY) * 0.0625D, (flash.zOffset() - bonePivotZ) * 0.0625D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.random * 360.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.random >= 0.5F ? 180.0F : 0.0F));
        renderMuzzleFlashQuad(poseStack, bufferSource, held, flash);
        poseStack.popPose();
    }

    private static void renderMuzzleFlashQuad(PoseStack poseStack, MultiBufferSource bufferSource, ItemStack held, MuzzleFlashProfile flash) {
        float size = (float) flash.size();
        poseStack.scale(size, size, 1.0F);
        poseStack.translate(-0.5F, -0.5F, 0.0F);

        boolean alternateFlash = held.isEnchanted() || usesAlternateMuzzleFlash(held);
        float minU = alternateFlash ? 0.5F : 0.0F;
        float maxU = alternateFlash ? 1.0F : 0.5F;
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

        return eye.add(look.scale(forwardMul + THIRD_PERSON_MUZZLE_FORWARD_OFFSET)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }

    private static MuzzleFlashProfile muzzleFlashProfile(ItemStack held) {
        if (held.getItem() instanceof GunItem gun) {
            return MUZZLE_FLASH_PROFILES.getOrDefault(gun.getStats().id().getPath(), DEFAULT_MUZZLE_FLASH);
        }
        return DEFAULT_MUZZLE_FLASH;
    }

    private static boolean usesAlternateMuzzleFlash(ItemStack held) {
        return held.getItem() instanceof GunItem gun && ALT_MUZZLE_FLASH_IDS.contains(gun.getStats().id().getPath());
    }
}
