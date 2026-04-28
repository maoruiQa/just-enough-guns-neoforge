package ttv.migami.jeg.network;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;

public final class NetworkHandler {
    private NetworkHandler() {}

    private static boolean commonRegistered;
    private static final Set<UUID> AIMING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> HOLD_FIRE_START_TICKS = new HashMap<>();

    public static void initCommon() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        PayloadTypeRegistry.serverboundPlay().register(ShootRequestPayload.TYPE, ShootRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(HoldFirePayload.TYPE, HoldFirePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReloadRequestPayload.TYPE, ReloadRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UnloadMagazineRequestPayload.TYPE, UnloadMagazineRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AimingStatePayload.TYPE, AimingStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BulletTrailPayload.TYPE, BulletTrailPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GunFireFxPayload.TYPE, GunFireFxPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OffhandFullPromptPayload.TYPE, OffhandFullPromptPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UiConfigPayload.TYPE, UiConfigPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ShootRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleShootRequest(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(HoldFirePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleHoldFire(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ReloadRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleReloadRequest(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(UnloadMagazineRequestPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleUnloadMagazineRequest(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(TriggerReleasePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleTriggerRelease(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(AimingStatePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleAimingState(payload, context.player()));
        });
    }

    private static void handleShootRequest(ShootRequestPayload payload, ServerPlayer player) {
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
    }

    private static void handleHoldFire(HoldFirePayload payload, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!payload.holding() || !GunItem.isHoldToFireWeapon(stack)) {
            HOLD_FIRE_START_TICKS.remove(player.getUUID());
            return;
        }
        HOLD_FIRE_START_TICKS.put(player.getUUID(), player.level().getGameTime() - 1L);
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

    private static void handleReloadRequest(ReloadRequestPayload payload, ServerPlayer player) {
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
                player.swing(payload.hand());
            }
            return;
        }

        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        boolean reloaded = gun.tryReload(player.level(), player, stack, true);
        if (reloaded) {
            player.swing(payload.hand());
        }
    }

    private static void handleUnloadMagazineRequest(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MagazineItem magazine)) {
            return;
        }

        MagazineItem.UnloadResult result = magazine.tryUnloadToOffhand(player.level(), player, stack);
        if (result.showOffhandFullPrompt()) {
            ServerPlayNetworking.send(player, OffhandFullPromptPayload.INSTANCE);
        }
        if (result.transferredAmmo()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isCoolant(ItemStack stack) {
        return stack.is(ModItems.COOLANT.get()) || stack.is(ModItems.ENHANCED_COOLANT.get());
    }

    private static void handleTriggerRelease(TriggerReleasePayload payload, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        if (GunItem.isAutomatic(gun.getStats())) {
            return;
        }
        GunItem.clearTriggerLock(stack);
    }

    private static void handleAimingState(AimingStatePayload payload, ServerPlayer player) {
        if (payload.aiming()) {
            AIMING_PLAYERS.add(player.getUUID());
        } else {
            AIMING_PLAYERS.remove(player.getUUID());
        }
    }

    public static boolean isAiming(Player player) {
        return AIMING_PLAYERS.contains(player.getUUID());
    }

    public static boolean shouldRenderLegacyBulletTrail() {
        return Config.legacyBulletTrailEnabled();
    }

    public static void sendGunFireFx(ServerLevel level, int shooterId, float randomValue) {
        GunFireFxPayload payload = new GunFireFxPayload(shooterId, randomValue);
        for (ServerPlayer player : PlayerLookup.level(level)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendHitMarker(ServerPlayer player, boolean critical) {
        ServerPlayNetworking.send(player, new HitMarkerPayload(critical));
    }

    public static void sendUiConfig(ServerPlayer player) {
        ServerPlayNetworking.send(player, new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback()));
    }

    public static void broadcastUiConfig(MinecraftServer server) {
        UiConfigPayload payload = new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback());
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
