package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.network.NetworkHandler;

public final class FabricClientBootstrap {
    private static final Identifier MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final int OVERHEAT_BAR_WIDTH = 82;
    private static final int OVERHEAT_BAR_HEIGHT = 4;
    private static final int OVERHEAT_BAR_Y_OFFSET = 58;
    private static boolean registered;
    private static int hudTicker;
    private static String lastHudText = "";
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static long nextVisualShotTickMain;
    private static final Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new ConcurrentHashMap<>();
    private static StunRingingSound stunRingingSound;

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

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
            if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
                BulletTrailRenderer.render(context.matrices(), context.consumers(), partialTick);
            }
            renderMuzzleFlashes(context.matrices(), context.consumers(), partialTick);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GunRecoilHandler.tick();
            if (NetworkHandler.shouldRenderLegacyBulletTrail()) {
                BulletTrailRenderer.tick();
            }
            tickMuzzleFlashState();

            handleCombatInput(client);
            suppressSwingAnimation(client);
            tickAmmoActionbar(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BulletTrailRenderer.clear();
            MUZZLE_FLASHES.clear();
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            nextVisualShotTickMain = 0L;
            stunRingingSound = null;
            AimingHandler.get().reset();
            GunRecoilHandler.stopImmediate();
        });
    }

    private static void handleCombatInput(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            nextVisualShotTickMain = 0L;
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

        if (KeyBindings.RELOAD.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.MAIN_HAND);
                attackHeldLastTick = false;
                nextVisualShotTickMain = 0L;
                GunRecoilHandler.stopImmediate();
            } else if (heldOff.getItem() instanceof GunItem) {
                ClientNetworkHandler.sendReload(InteractionHand.OFF_HAND);
            }
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
        float recoilMultiplier = RecoilProfiles.multiplier(gun.getStats().id());
        float recoilKick = gun.getStats().recoilKick() * recoilMultiplier;
        float targetPitch = player.getXRot() - recoilKick * getPitchKickMultiplier(gun);
        player.setXRot(Math.max(-90.0F, Math.min(90.0F, targetPitch)));
        int shotsPerTrigger = "minigun".equals(gun.getStats().id().getPath()) ? 5 : 1;
        for (int i = 0; i < shotsPerTrigger; i++) {
            GunRecoilHandler.addShot(recoilKick * 2.20F);
        }
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

    private static void tickAmmoActionbar(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            hudTicker = 0;
            lastHudText = "";
            return;
        }

        hudTicker++;
        if (hudTicker % 4 != 0) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gun)) {
            lastHudText = "";
            return;
        }

        String hudText = buildAmmoHudText(player, held, gun);
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

    public static void renderOverheatBar(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gun) || !gun.usesOverheatMechanic()) {
            return;
        }

        int percent = gun.getOverheatPercent(held);
        if (percent <= 0) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = (screenWidth - OVERHEAT_BAR_WIDTH) / 2;
        int y = screenHeight - OVERHEAT_BAR_Y_OFFSET;

        guiGraphics.fill(x - 1, y - 1, x + OVERHEAT_BAR_WIDTH + 1, y + OVERHEAT_BAR_HEIGHT + 1, 0xAA000000);
        guiGraphics.fill(x, y, x + OVERHEAT_BAR_WIDTH, y + OVERHEAT_BAR_HEIGHT, 0x66000000);

        float ratio = Math.max(0.0F, Math.min(1.0F, percent / 100.0F));
        int fill = Math.max(1, Math.round(OVERHEAT_BAR_WIDTH * ratio));
        guiGraphics.fill(x, y, x + fill, y + OVERHEAT_BAR_HEIGHT, overheatColor(ratio));

        if (gun.shouldShowWaterCoolingPrompt(held)) {
            renderWaterCoolingPrompt(guiGraphics, minecraft, x, y);
        }

        ItemStack offhand = player.getOffhandItem();
        ItemStack mainhand = player.getMainHandItem();
        if (GunItem.canWaterCool(mainhand) && GunItem.isCoolingWithWater(mainhand)) {
            renderWaterCoolingBar(guiGraphics, x, y - 6, mainhand);
        } else if (GunItem.canWaterCool(offhand) && GunItem.isCoolingWithWater(offhand)) {
            renderWaterCoolingBar(guiGraphics, x, y - 6, offhand);
        }
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

    private static int overheatColor(float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        int red;
        int green;
        if (clamped < 0.5F) {
            float t = clamped / 0.5F;
            red = Math.round(255.0F * t);
            green = 255;
        } else {
            float t = (clamped - 0.5F) / 0.5F;
            red = 255;
            green = Math.round(255.0F * (1.0F - t));
        }
        int blue = 48;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void renderWaterCoolingPrompt(GuiGraphics guiGraphics, Minecraft minecraft, int x, int y) {
        Component text = Component.translatable("jeg.water_cooling.prompt");
        int textX = (minecraft.getWindow().getGuiScaledWidth() - minecraft.font.width(text)) / 2;
        int textY = y - 12;
        guiGraphics.fill(textX - 2, textY - 2, textX + minecraft.font.width(text) + 2, textY + minecraft.font.lineHeight + 2, 0x66000000);
        minecraft.gui.setOverlayMessage(text, false);
    }

    private static void renderWaterCoolingBar(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        float coolingRatio = GunItem.getWaterCoolingProgressPercent(stack) / 100.0F;
        int coolingFill = Math.max(1, Math.round(OVERHEAT_BAR_WIDTH * coolingRatio));
        guiGraphics.fill(x - 1, y - 1, x + OVERHEAT_BAR_WIDTH + 1, y + OVERHEAT_BAR_HEIGHT + 1, 0xAA000000);
        guiGraphics.fill(x, y, x + OVERHEAT_BAR_WIDTH, y + OVERHEAT_BAR_HEIGHT, 0x66000000);
        guiGraphics.fill(x, y, x + coolingFill, y + OVERHEAT_BAR_HEIGHT, coolingColor(coolingRatio));
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
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutoutNoCull(MUZZLE_FLASH_TEXTURE));

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

        double forwardMul = 0.78D;
        double sideMul = 0.06D;
        double heightMul = -0.05D;
        if (held.getItem() instanceof GunItem gun) {
            switch (GunCategory.fromStats(gun.getStats())) {
                case RIFLE -> forwardMul = 1.00D;
                case SHOTGUN -> forwardMul = 1.05D;
                case SNIPER -> forwardMul = 1.12D;
                case LMG -> forwardMul = 1.08D;
                case HEAVY -> {
                    forwardMul = 1.20D;
                    heightMul = -0.02D;
                }
                default -> {
                    // Pistols/SMGs/special keep the baseline offset.
                }
            }
        }

        if (shooter instanceof Player player && player.getMainArm() == HumanoidArm.LEFT) {
            sideMul *= -1.0D;
        }

        return eye.add(look.scale(forwardMul)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }
}
