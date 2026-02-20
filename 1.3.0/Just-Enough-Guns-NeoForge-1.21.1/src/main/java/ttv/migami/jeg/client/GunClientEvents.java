package ttv.migami.jeg.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.NetworkHandler;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final float ADS_FOV_FACTOR = 0.35F;
    private static int hudTicker;
    private static String lastHudText = "";
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static long nextVisualShotTickMain;

    private GunClientEvents() {}

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

        ItemStack heldMain = player.getMainHandItem();
        if (!(heldMain.getItem() instanceof GunItem)) {
            return;
        }

        if (event.isUseItem() || event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GunRecoilHandler.tick();
        if (Config.legacyBulletTrailEnabled()) {
            ttv.migami.jeg.client.render.BulletTrailRenderer.tick();
        }

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
                if (shouldApplyVisualRecoil(gun, attackHeldLastTick, nowTick)) {
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
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (Config.legacyBulletTrailEnabled()) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            ttv.migami.jeg.client.render.BulletTrailRenderer.render(
                    new PoseStack(),
                    minecraft.renderBuffers().bufferSource(),
                    partialTick
            );
        }
    }

    private static String buildAmmoHudText(LocalPlayer player, ItemStack stack, GunItem gun) {
        GunStats stats = gun.getStats();
        int reserve = gun.countInventoryAmmo(player);
        boolean infinite = reserve == Integer.MAX_VALUE;
        String reserveText = infinite ? "∞" : Integer.toString(Math.max(0, reserve));

        if (stats.usesMagazine()) {
            int magazine = gun.getMagazineAmmo(stack);
            return "Ammo " + magazine + "/" + stats.magazineSize() + " | Reserve " + reserveText;
        }
        return "Ammo " + reserveText;
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ttv.migami.jeg.client.render.BulletTrailRenderer.clear();
        attackHeldLastTick = false;
        aimingStateLastSent = false;
        nextVisualShotTickMain = 0L;
    }

    private static boolean shouldApplyVisualRecoil(GunItem gun, boolean attackHeldLastTick, long nowTick) {
        int fireDelay = Math.max(1, gun.getStats().fireDelay());
        if (!gun.isAutomatic()) {
            if (attackHeldLastTick) {
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

    private static void applyLocalVisualRecoil(GunItem gun) {
        float recoilMultiplier = RecoilProfiles.multiplier(gun.getStats().id());
        float recoilKick = gun.getStats().recoilKick() * recoilMultiplier;
        int shotsPerTrigger = "minigun".equals(gun.getStats().id().getPath()) ? 5 : 1;
        for (int i = 0; i < shotsPerTrigger; i++) {
            GunRecoilHandler.addShot(recoilKick * 2.20F);
        }
    }
}
