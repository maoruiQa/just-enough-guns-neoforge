package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerGeoRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomGeoRenderer;
import ttv.migami.jeg.compat.ClientHooks;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.AimingStatePayload;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.network.ReloadRequestPayload;
import ttv.migami.jeg.network.ShootRequestPayload;
import ttv.migami.jeg.network.TriggerReleasePayload;

public final class FabricClientBootstrap {
    private static boolean registered;
    private static boolean attackHeldLastTick;
    private static boolean aimingStateLastSent;
    private static boolean reloadHeldLastTick;
    private static long nextVisualShotTickMain;
    private static int hudTicker;
    private static String lastHudText = "";

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
            public void addBulletTrail(net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 end, int color, float size) {
                BulletTrailRenderer.addInstantTrail(start, end, color, size);
            }
        });
        ClientNetworkHandler.initClient();
        registerClientExtensions(new RegisterClientExtensionsEvent());

        EntityRendererRegistry.register(ModEntities.BULLET.get(), BulletRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomGeoRenderer::new);
        EntityRendererRegistry.register(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomGeoRenderer::new);

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
                player != null && player.getMainHandItem().getItem() instanceof GunItem);

        ClientTickEvents.END_CLIENT_TICK.register(FabricClientBootstrap::onClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BulletTrailRenderer.clear();
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

        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            attackHeldLastTick = false;
            aimingStateLastSent = false;
            reloadHeldLastTick = false;
            nextVisualShotTickMain = 0L;
            hudTicker = 0;
            lastHudText = "";
            AimingHandler.get().reset();
            GunRecoilHandler.stopImmediate();
            return;
        }

        AimingHandler.get().tick(player);
        boolean aiming = AimingHandler.get().isAiming();
        if (aiming != aimingStateLastSent) {
            aimingStateLastSent = aiming;
            ClientPlayNetworking.send(new AimingStatePayload(aiming));
        }

        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();

        if (heldMain.getItem() instanceof GunItem gun) {
            boolean attackDown = client.options.keyAttack.isDown();
            long nowTick = player.level().getGameTime();

            if (attackDown) {
                if (gun.isAutomatic() || !attackHeldLastTick) {
                    ClientPlayNetworking.send(new ShootRequestPayload(InteractionHand.MAIN_HAND));
                }
                if (shouldApplyVisualRecoil(gun, attackHeldLastTick, nowTick)) {
                    applyLocalVisualRecoil(gun);
                }
            } else if (attackHeldLastTick && !gun.isAutomatic() && GunItem.isTriggerLocked(heldMain)) {
                GunItem.clearTriggerLock(heldMain);
                ClientPlayNetworking.send(new TriggerReleasePayload(InteractionHand.MAIN_HAND));
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
                ClientPlayNetworking.send(new ReloadRequestPayload(InteractionHand.MAIN_HAND));
            } else if (heldOff.getItem() instanceof GunItem) {
                ClientPlayNetworking.send(new ReloadRequestPayload(InteractionHand.OFF_HAND));
            }
        }
        reloadHeldLastTick = reloadDown;

        suppressSwingAnimation(player, heldMain, heldOff);
        tickAmmoActionbar(player, heldMain);
    }

    private static boolean shouldApplyVisualRecoil(GunItem gun, boolean wasHeldLastTick, long nowTick) {
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

    private static void applyLocalVisualRecoil(GunItem gun) {
        float recoilMultiplier = RecoilProfiles.multiplier(gun.getStats().id());
        float recoilKick = gun.getStats().recoilKick() * recoilMultiplier;
        int shotsPerTrigger = "minigun".equals(gun.getStats().id().getPath()) ? 5 : 1;
        for (int i = 0; i < shotsPerTrigger; i++) {
            GunRecoilHandler.addShot(recoilKick * 2.20F);
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
}
