package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
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
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.NetworkHandler;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class GunClientEvents {
    private static final float ADS_FOV_FACTOR = 0.35F;
    private static int hudTicker;
    private static String lastHudText = "";
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;

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
        float ads = AimingHandler.get().getNormalisedAdsProgress(1.0F);
        if (ads <= 0.0F) {
            return;
        }
        float factor = 1.0F - ADS_FOV_FACTOR * ads;
        event.setNewFovModifier(Math.max(0.1F, event.getNewFovModifier() * factor));
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }
        if (event.isUseItem() || event.isAttack()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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

        if (heldMain.getItem() instanceof GunItem gun) {
            boolean attackDown = minecraft.options.keyAttack.isDown();
            if (attackDown) {
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    NetworkHandler.sendShoot(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            } else if (attackHeldLastTick && !gun.isAutomatic() && GunItem.isTriggerLocked(heldMain)) {
                GunItem.clearTriggerLock(heldMain);
                NetworkHandler.sendTriggerRelease(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            attackHeldLastTick = attackDown;
        } else {
            attackHeldLastTick = false;
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

        if (heldMain.getItem() instanceof GunItem gun) {
            String hudText = buildAmmoHudText(player, heldMain, gun);
            if (!hudText.equals(lastHudText) || hudTicker % 20 == 0) {
                player.displayClientMessage(buildAmmoHudComponent(heldMain, hudText), true);
                lastHudText = hudText;
            }
        } else {
            lastHudText = "";
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
                    new com.mojang.blaze3d.vertex.PoseStack(),
                    minecraft.renderBuffers().bufferSource(),
                    partialTick
            );
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ttv.migami.jeg.client.render.BulletTrailRenderer.clear();
        hudTicker = 0;
        lastHudText = "";
        attackHeldLastTick = false;
        aimingStateLastSent = false;
    }

    private static String buildAmmoHudText(LocalPlayer player, ItemStack stack, GunItem gun) {
        int reserve = gun.countInventoryAmmo(player);
        String reserveText = reserve == Integer.MAX_VALUE ? "\u221e" : Integer.toString(Math.max(0, reserve));
        if (gun.getStats().usesMagazine()) {
            int magazine = gun.getMagazineAmmo(stack);
            return magazine + "/" + gun.getStats().magazineSize() + " | " + reserveText;
        }
        return reserveText;
    }

    private static Component buildAmmoHudComponent(ItemStack stack, String ammoText) {
        MutableComponent gunName = stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
        MutableComponent divider = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY);
        MutableComponent ammo = Component.literal(ammoText).withStyle(ChatFormatting.GOLD);
        return gunName.append(divider).append(ammo);
    }
}
