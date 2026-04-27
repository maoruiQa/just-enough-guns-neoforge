package ttv.migami.jeg.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.CrosshairHandler;
import ttv.migami.jeg.client.GunClientEvents;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;

public final class NetworkHandler {
    private NetworkHandler() {}
    private static final String AIMING_TAG = "jeg_aiming";
    private static final Map<UUID, Long> HOLD_FIRE_START_TICKS = new HashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Reference.MOD_ID)
                .playToServer(ShootRequestPayload.TYPE, ShootRequestPayload.STREAM_CODEC, NetworkHandler::handleShootRequest)
                .playToServer(HoldFirePayload.TYPE, HoldFirePayload.STREAM_CODEC, NetworkHandler::handleHoldFire)
                .playToServer(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC, NetworkHandler::handleTriggerRelease)
                .playToServer(ReloadRequestPayload.TYPE, ReloadRequestPayload.STREAM_CODEC, NetworkHandler::handleReloadRequest)
                .playToServer(UnloadMagazineRequestPayload.TYPE, UnloadMagazineRequestPayload.STREAM_CODEC, NetworkHandler::handleUnloadMagazineRequest)
                .playToServer(AimingStatePayload.TYPE, AimingStatePayload.STREAM_CODEC, NetworkHandler::handleAimingState)
                .playToClient(BulletTrailPayload.TYPE, BulletTrailPayload.STREAM_CODEC, NetworkHandler::handleBulletTrail)
                .playToClient(GunFireFxPayload.TYPE, GunFireFxPayload.STREAM_CODEC, NetworkHandler::handleGunFireFx)
                .playToClient(OffhandFullPromptPayload.TYPE, OffhandFullPromptPayload.STREAM_CODEC, NetworkHandler::handleOffhandFullPrompt)
                .playToClient(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC, NetworkHandler::handleHitMarker);
    }

    private static void handleBulletTrail(BulletTrailPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!Config.legacyBulletTrailEnabled()) {
                return;
            }
            BulletTrailRenderer.upsertLegacyTrail(payload);
        });
    }

    private static void handleAimingState(AimingStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                player.getPersistentData().putBoolean(AIMING_TAG, payload.aiming());
            }
        });
    }

    private static void handleGunFireFx(GunFireFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunClientEvents.showMuzzleFlash(payload.shooterId(), payload.randomValue()));
    }

    private static void handleOffhandFullPrompt(OffhandFullPromptPayload payload, IPayloadContext context) {
        context.enqueueWork(GunClientEvents::showOffhandFullPrompt);
    }

    private static void handleHitMarker(HitMarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CrosshairHandler.playHitMarker(payload.critical()));
    }

    private static void handleTriggerRelease(TriggerReleasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof GunItem gun)) {
                return;
            }
            if (GunItem.isAutomatic(gun.getStats())) {
                return;
            }
            GunItem.clearTriggerLock(stack);
        });
    }

    private static void handleHoldFire(HoldFirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!payload.holding() || !GunItem.isHoldToFireWeapon(stack)) {
                HOLD_FIRE_START_TICKS.remove(player.getUUID());
                return;
            }
            HOLD_FIRE_START_TICKS.put(player.getUUID(), player.level().getGameTime() - 1L);
        });
    }

    private static void handleShootRequest(ShootRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof GunItem gun)) {
                return;
            }

            if (GunItem.isHoldToFireWeapon(stack) && !hasCompletedHoldFire(player, stack)) {
                return;
            }

            boolean shot = gun.tryShoot(player.level(), player, payload.hand());
            if (shot && GunItem.isHoldToFireWeapon(stack)) {
                HOLD_FIRE_START_TICKS.remove(player.getUUID());
            }
        });
    }

    private static boolean hasCompletedHoldFire(ServerPlayer player, ItemStack stack) {
        Long startTick = HOLD_FIRE_START_TICKS.get(player.getUUID());
        if (startTick == null) {
            return false;
        }
        long elapsed = player.level().getGameTime() - startTick;
        if (elapsed > 200L) {
            HOLD_FIRE_START_TICKS.remove(player.getUUID());
            return false;
        }
        return elapsed >= GunItem.holdToFireTicks(stack);
    }

    private static void handleReloadRequest(ReloadRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack offhand = player.getOffhandItem();
            ItemStack mainHand = player.getMainHandItem();
            if (payload.hand() == InteractionHand.MAIN_HAND
                    && mainHand.getItem() instanceof GunItem
                    && isCoolant(offhand)
                    && GunItem.tryStartWaterCooling(player.level(), player, InteractionHand.OFF_HAND)) {
                player.startUsingItem(InteractionHand.OFF_HAND);
                return;
            }

            ItemStack stack = player.getItemInHand(payload.hand());
            if (payload.hand() == InteractionHand.MAIN_HAND && stack.getItem() instanceof MagazineItem magazine) {
                boolean notify = magazine.getLoadPromptMessage(stack, player.getOffhandItem()) == null;
                boolean loaded = magazine.tryLoad(player.level(), player, stack, player.getOffhandItem(), notify);
                if (loaded) {
                    player.swing(payload.hand(), true);
                }
                return;
            }

            if (!(stack.getItem() instanceof GunItem gun)) {
                return;
            }
            boolean reloaded = gun.tryReload(player.level(), player, stack, true);
            if (reloaded) {
                player.swing(payload.hand(), true);
            }
        });
    }

    private static void handleUnloadMagazineRequest(UnloadMagazineRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof MagazineItem magazine)) {
                return;
            }

            MagazineItem.UnloadResult result = magazine.tryUnloadToOffhand(player.level(), player, stack);
            if (result.showOffhandFullPrompt()) {
                player.connection.send(OffhandFullPromptPayload.INSTANCE);
            }
            if (result.transferredAmmo()) {
                player.swing(InteractionHand.MAIN_HAND, true);
            }
        });
    }

    private static boolean isCoolant(ItemStack stack) {
        return stack.is(ModItems.COOLANT.get()) || stack.is(ModItems.ENHANCED_COOLANT.get());
    }

    public static void sendTriggerRelease(InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new TriggerReleasePayload(hand));
    }

    public static void sendReload(InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new ReloadRequestPayload(hand));
    }

    public static void sendShoot(InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new ShootRequestPayload(hand));
    }

    public static void sendHoldFire(InteractionHand hand, boolean holding) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new HoldFirePayload(hand, holding));
    }

    public static void sendAiming(boolean aiming) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new AimingStatePayload(aiming));
    }

    public static void sendUnloadMagazine() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(UnloadMagazineRequestPayload.INSTANCE);
    }

    public static void sendGunFireFx(ServerLevel level, int shooterId, float randomValue) {
        GunFireFxPayload payload = new GunFireFxPayload(shooterId, randomValue);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) {
                player.connection.send(payload);
            }
        }
    }

    public static void sendHitMarker(ServerPlayer player, boolean critical) {
        player.connection.send(new HitMarkerPayload(critical));
    }

    public static boolean isAiming(Player player) {
        return player.getPersistentData().getBoolean(AIMING_TAG).orElse(false);
    }
}
