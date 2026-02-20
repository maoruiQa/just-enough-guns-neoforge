package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerGeoRenderer;
import ttv.migami.jeg.client.render.entity.RaidEntityRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomGeoRenderer;
import ttv.migami.jeg.compat.ClientHooks;
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.ClientNetworkHandler;

public final class FabricClientBootstrap {
    private static final ResourceLocation MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static boolean registered;
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean reloadHeldLastTick;
    private static long nextVisualShotTickMain;
    private static int hudTicker;
    private static String lastHudText = "";
    private static final Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new ConcurrentHashMap<>();

    private static final class MuzzleFlashState {
        private int ticksRemaining;
        private final float random;

        private MuzzleFlashState(int ticksRemaining, float random) {
            this.ticksRemaining = ticksRemaining;
            this.random = random;
        }
    }

    private FabricClientBootstrap() {}

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;

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

        EntityRendererRegistry.register(ModEntities.BULLET.get(), BulletRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.RAID_ENTITY.get(), RaidEntityRenderer::new);

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
                player != null && player.getMainHandItem().getItem() instanceof GunItem);

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            PoseStack matrixStack = context.matrixStack();
            if (matrixStack == null) {
                return;
            }
            renderMuzzleFlashes(matrixStack, context.consumers(), partialTick);
        });

        ClientTickEvents.END_CLIENT_TICK.register(FabricClientBootstrap::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BulletTrailRenderer.clear();
            MUZZLE_FLASHES.clear();
            AimingHandler.get().reset();
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            reloadHeldLastTick = false;
            nextVisualShotTickMain = 0L;
            GunRecoilHandler.stopImmediate();
        });
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (var holder : ModItems.GUNS.values()) {
            event.registerItem(new GunItemClientExtensions(holder.get()), holder.get());
        }
    }

    private static void onClientTick(Minecraft client) {
        GunRecoilHandler.tick();
        BulletTrailRenderer.tick();
        tickMuzzleFlashState();

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            reloadHeldLastTick = false;
            nextVisualShotTickMain = 0L;
            hudTicker = 0;
            lastHudText = "";
            MUZZLE_FLASHES.clear();
            AimingHandler.get().reset();
            GunRecoilHandler.stopImmediate();
            return;
        }

        AimingHandler.get().tick(player);
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

            if (attackDown) {
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    ClientNetworkHandler.sendShoot(InteractionHand.MAIN_HAND);
                }
                if (shouldApplyVisualRecoil(player, heldMain, gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(player, gun);
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
            GunRecoilHandler.stopImmediate();
        }

        boolean reloadDown = InputConstants.isKeyDown(client.getWindow().getWindow(), GLFW.GLFW_KEY_R);
        if (reloadDown && !reloadHeldLastTick) {
            if (heldMain.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
            } else if (heldOff.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.OFF_HAND);
            }
        }
        reloadHeldLastTick = reloadDown;

        suppressSwingAnimation(player, heldMain, heldOff);
        tickAmmoActionbar(player, heldMain);
    }

    private static boolean shouldApplyVisualRecoil(LocalPlayer player, ItemStack stack, GunItem gun, boolean wasHeldLastTick, long nowTick) {
        if (!hasShootableAmmo(player, stack, gun)) {
            return false;
        }
        int fireDelay = Math.max(1, gun.getStats().fireDelay());
        if (!gun.isAutomatic()) {
            if (wasHeldLastTick) {
                return false;
            }
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
        float recoilMultiplier = RecoilProfiles.multiplier(gun.getStats().id());
        float recoilKick = gun.getStats().recoilKick() * recoilMultiplier;
        float targetPitch = player.getXRot() - recoilKick * getPitchKickMultiplier(gun);
        player.setXRot(Math.max(-90.0F, Math.min(90.0F, targetPitch)));
        int shotsPerTrigger = "minigun".equals(gun.getStats().id().getPath()) ? 5 : 1;
        for (int i = 0; i < shotsPerTrigger; i++) {
            GunRecoilHandler.addShot(recoilKick * 2.20F);
        }
    }

    private static boolean hasShootableAmmo(LocalPlayer player, ItemStack stack, GunItem gun) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        GunStats stats = gun.getStats();
        if (stats.isInventoryFed() || !stats.usesMagazine()) {
            return gun.countInventoryAmmo(player) > 0;
        }
        return gun.getMagazineAmmo(stack) > 0;
    }

    private static float getPitchKickMultiplier(GunItem gun) {
        String path = gun.getStats().id().getPath();
        if ("rocket_launcher".equals(path) || "typhoonee".equals(path)) {
            return 4.5F;
        }
        if ("minigun".equals(path)) {
            return 1.2F;
        }
        return 3.0F;
    }

    private static void suppressSwingAnimation(LocalPlayer player, ItemStack main, ItemStack off) {
        if (main.getItem() instanceof GunItem || off.getItem() instanceof GunItem) {
            player.attackAnim = 0.0F;
            player.oAttackAnim = 0.0F;
            player.swingTime = 0;
            player.swinging = false;
        }
    }

    private static void tickAmmoActionbar(LocalPlayer player, ItemStack heldMain) {
        hudTicker++;
        if (hudTicker % 4 != 0) {
            return;
        }

        if (!(heldMain.getItem() instanceof GunItem gun)) {
            lastHudText = "";
            return;
        }

        String hudText = buildAmmoHudText(player, heldMain, gun);
        if (!hudText.isEmpty() && (!hudText.equals(lastHudText) || hudTicker % 20 == 0)) {
            player.displayClientMessage(Component.literal(hudText), true);
            lastHudText = hudText;
        }
    }

    private static String buildAmmoHudText(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunStats stats = gun.getStats();
        int reserve = gun.countInventoryAmmo(player);
        String reserveText = reserve == Integer.MAX_VALUE ? "INF" : Integer.toString(Math.max(0, reserve));

        if (stats.usesMagazine()) {
            int magazine = gun.getMagazineAmmo(stack);
            return "Ammo " + magazine + "/" + stats.magazineSize() + " | Reserve " + reserveText;
        }
        return "Ammo " + reserveText;
    }

    public static void showMuzzleFlash(int entityId, float random) {
        MUZZLE_FLASHES.put(entityId, new MuzzleFlashState(2, random));
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

            ItemStack held = living.getMainHandItem();
            if (!(held.getItem() instanceof GunItem)) {
                continue;
            }

            Vec3 muzzlePos = computeMuzzlePosition(living, held, partialTick);
            poseStack.pushPose();
            poseStack.translate(muzzlePos.x - cameraPos.x, muzzlePos.y - cameraPos.y, muzzlePos.z - cameraPos.z);
            poseStack.mulPose(camera.rotation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(entry.getValue().random * 360.0F));

            float size = 0.45F + entry.getValue().random * 0.25F;
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

        double forwardMul = 0.66D;
        double sideMul = 0.06D;
        double heightMul = -0.05D;
        if (held.getItem() instanceof GunItem gun) {
            switch (GunCategory.fromStats(gun.getStats())) {
                case RIFLE -> forwardMul = 0.84D;
                case SHOTGUN -> forwardMul = 0.88D;
                case SNIPER -> forwardMul = 0.95D;
                case LMG -> forwardMul = 0.90D;
                case HEAVY -> {
                    forwardMul = 1.00D;
                    heightMul = -0.02D;
                }
                default -> {
                    // Keep baseline for pistols/SMGs/special.
                }
            }
        }

        if (shooter instanceof Player player && player.getMainArm() == HumanoidArm.LEFT) {
            sideMul *= -1.0D;
        }

        return eye.add(look.scale(forwardMul)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }
}
