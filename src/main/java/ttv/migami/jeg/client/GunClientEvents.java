package ttv.migami.jeg.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import ttv.migami.jeg.gun.GunCategory;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.NetworkHandler;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final float ADS_FOV_FACTOR = 0.35F;
    private static final ResourceLocation MUZZLE_FLASH_TEXTURE = Reference.id("textures/effect/muzzle_flash.png");
    private static final int OVERHEAT_BAR_WIDTH = 82;
    private static final int OVERHEAT_BAR_HEIGHT = 4;
    private static int hudTicker;
    private static String lastHudText = "";
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static long nextVisualShotTickMain;
    private static final java.util.Map<Integer, MuzzleFlashState> MUZZLE_FLASHES = new java.util.concurrent.ConcurrentHashMap<>();
    private static StunRingingSound stunRingingSound;

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
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress();
        if (ads <= 0.0F) {
            return;
        }
        float factor = 1.0F - ADS_FOV_FACTOR * ads;
        event.setNewFovModifier(Math.max(0.1F, event.getNewFovModifier() * factor));
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
        if (held.getItem() instanceof GunItem gun && gun.usesOverheatMechanic()) {
            int heatPercent = gun.getOverheatPercent(held);
            if (heatPercent > 0) {
                renderOverheatBar(event.getGuiGraphics(), heatPercent);
                if (gun.shouldShowWaterCoolingPrompt(held)) {
                    renderWaterCoolingPrompt(event.getGuiGraphics());
                }
            }
        }

        ItemStack offhand = player.getOffhandItem();
        if (GunItem.canWaterCool(held) && GunItem.isCoolingWithWater(held)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(held));
        } else if (GunItem.canWaterCool(offhand) && GunItem.isCoolingWithWater(offhand)) {
            renderWaterCoolingBar(event.getGuiGraphics(), GunItem.getWaterCoolingProgressPercent(offhand));
        }

        if (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem) {
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

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            hudTicker = 0;
            lastHudText = "";
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
            if (attackDown) {
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    NetworkHandler.sendShoot(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
                if (shouldApplyVisualRecoil(player, heldMain, gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(gun);
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
            GunRecoilHandler.stopImmediate();
        }

        // R key reload (server-authoritative). Keep swap-hands reload as fallback/compat.
        if (KeyBindings.RELOAD.consumeClick()) {
            if (heldMain.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.MAIN_HAND);
            } else if (heldOff.getItem() instanceof GunItem) {
                NetworkHandler.sendReload(net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }

        hudTicker++;
        if (hudTicker % 4 != 0) {
            return;
        }

        ItemStack held = heldMain;
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

    private static String buildAmmoHudText(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunStats stats = gun.getStats();
        int reserve = gun.countInventoryAmmo(player);
        boolean infinite = reserve == Integer.MAX_VALUE;
        String reserveText = infinite ? "INF" : Integer.toString(Math.max(0, reserve));

        if (stats.usesMagazine()) {
            int magazine = gun.getMagazineAmmo(stack);
            return "Ammo " + magazine + "/" + stats.magazineSize() + " | Reserve " + reserveText;
        }

        return "Ammo " + reserveText;
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
        nextVisualShotTickMain = 0L;
        stunRingingSound = null;
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
        float recoilMultiplier = RecoilProfiles.multiplier(gun.getStats().id());
        float recoilKick = gun.getStats().recoilKick() * recoilMultiplier;
        int shotsPerTrigger = "minigun".equals(gun.getStats().id().getPath()) ? 5 : 1;
        for (int i = 0; i < shotsPerTrigger; i++) {
            GunRecoilHandler.addShot(recoilKick * 2.20F);
        }
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
        GunStats stats = gun.getStats();
        if (stats.isInventoryFed() || !stats.usesMagazine()) {
            return gun.countInventoryAmmo(player) > 0;
        }
        return gun.getMagazineAmmo(stack) > 0;
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
                    // Pistols/SMGs/special keep the baseline offset.
                }
            }
        }

        if (shooter instanceof Player player && player.getMainArm() == HumanoidArm.LEFT) {
            sideMul *= -1.0D;
        }

        return eye.add(look.scale(forwardMul)).add(side.scale(sideMul)).add(0.0D, heightMul, 0.0D);
    }

    private static void renderOverheatBar(net.minecraft.client.gui.GuiGraphics guiGraphics, int heatPercent) {
        float ratio = Mth.clamp(heatPercent / 100.0F, 0.0F, 1.0F);
        int x = (guiGraphics.guiWidth() - OVERHEAT_BAR_WIDTH) / 2;
        int y = guiGraphics.guiHeight() - 66;
        int filled = Math.max(0, Mth.ceil(OVERHEAT_BAR_WIDTH * ratio));

        guiGraphics.fill(x - 1, y - 1, x + OVERHEAT_BAR_WIDTH + 1, y + OVERHEAT_BAR_HEIGHT + 1, 0xAA000000);
        guiGraphics.fill(x, y, x + OVERHEAT_BAR_WIDTH, y + OVERHEAT_BAR_HEIGHT, 0x66000000);

        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + OVERHEAT_BAR_HEIGHT, overheatColor(ratio));
        }
    }

    private static void renderWaterCoolingPrompt(net.minecraft.client.gui.GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Component text = Component.translatable("jeg.water_cooling.prompt");
        int x = (guiGraphics.guiWidth() - minecraft.font.width(text)) / 2;
        int y = guiGraphics.guiHeight() - 78;
        guiGraphics.fill(x - 2, y - 2, x + minecraft.font.width(text) + 2, y + minecraft.font.lineHeight + 2, 0x66000000);
        minecraft.gui.setOverlayMessage(text, false);
    }

    private static void renderWaterCoolingBar(net.minecraft.client.gui.GuiGraphics guiGraphics, int progressPercent) {
        float ratio = Mth.clamp(progressPercent / 100.0F, 0.0F, 1.0F);
        int x = (guiGraphics.guiWidth() - OVERHEAT_BAR_WIDTH) / 2;
        int y = guiGraphics.guiHeight() - 72;
        int filled = Math.max(0, Mth.ceil(OVERHEAT_BAR_WIDTH * ratio));

        guiGraphics.fill(x - 1, y - 1, x + OVERHEAT_BAR_WIDTH + 1, y + OVERHEAT_BAR_HEIGHT + 1, 0xAA000000);
        guiGraphics.fill(x, y, x + OVERHEAT_BAR_WIDTH, y + OVERHEAT_BAR_HEIGHT, 0x66000000);

        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + OVERHEAT_BAR_HEIGHT, coolingColor(ratio));
        }
    }

    private static int overheatColor(float ratio) {
        float clamped = Mth.clamp(ratio, 0.0F, 1.0F);
        int red;
        int green;
        if (clamped < 0.5F) {
            float t = clamped / 0.5F;
            red = Mth.floor(255.0F * t);
            green = 255;
        } else {
            float t = (clamped - 0.5F) / 0.5F;
            red = 255;
            green = Mth.floor(255.0F * (1.0F - t));
        }
        int blue = 32;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
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

}
