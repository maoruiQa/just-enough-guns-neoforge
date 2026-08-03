package ttv.migami.jeg.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.config.ServerConfigEditor;
import ttv.migami.jeg.event.AttachmentRuntimeEvents;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.item.GuidedLauncherItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;
import ttv.migami.jeg.vehicle.network.AssembleTestVehiclePayload;
import ttv.migami.jeg.vehicle.network.VehicleAssemblyRecipeSyncPayload;
import ttv.migami.jeg.vehicle.network.VehicleChangeSeatPayload;
import ttv.migami.jeg.vehicle.network.VehicleDataSyncPayload;
import ttv.migami.jeg.vehicle.network.VehicleDismountPayload;
import ttv.migami.jeg.vehicle.network.VehicleInputPayload;
import ttv.migami.jeg.vehicle.network.VehicleOpenMenuPayload;
import ttv.migami.jeg.vehicle.network.VehicleSeatAssignmentsPayload;
import ttv.migami.jeg.vehicle.network.VehicleStatePayload;

public final class NetworkHandler {
    private NetworkHandler() {}

    /** Fallback distance for vehicle state when not already tracking the entity (256 blocks). */
    private static final double VEHICLE_STATE_FALLBACK_DISTANCE_SQR = 256.0D * 256.0D;

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
        PayloadTypeRegistry.serverboundPlay().register(OpenAttachmentsPayload.TYPE, OpenAttachmentsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleMedalsPayload.TYPE, ToggleMedalsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MeleePayload.TYPE, MeleePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(InspectGunPayload.TYPE, InspectGunPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleFlashlightPayload.TYPE, ToggleFlashlightPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ChargeFlashlightPayload.TYPE, ChargeFlashlightPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AimingStatePayload.TYPE, AimingStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuidedLockPayload.TYPE, GuidedLockPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleLauncherModePayload.TYPE, ToggleLauncherModePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DroneInputPayload.TYPE, DroneInputPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VehicleInputPayload.TYPE, VehicleInputPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VehicleChangeSeatPayload.TYPE, VehicleChangeSeatPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VehicleDismountPayload.TYPE, VehicleDismountPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VehicleOpenMenuPayload.TYPE, VehicleOpenMenuPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AssembleTestVehiclePayload.TYPE, AssembleTestVehiclePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenServerConfigPayload.TYPE, OpenServerConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ApplyServerConfigPayload.TYPE, ApplyServerConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BulletTrailPayload.TYPE, BulletTrailPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GunFireFxPayload.TYPE, GunFireFxPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OffhandFullPromptPayload.TYPE, OffhandFullPromptPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(HeadshotMedalPayload.TYPE, HeadshotMedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MedalPayload.TYPE, MedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(KillMedalPayload.TYPE, KillMedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(UiConfigPayload.TYPE, UiConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VehicleDataSyncPayload.TYPE, VehicleDataSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VehicleAssemblyRecipeSyncPayload.TYPE, VehicleAssemblyRecipeSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VehicleStatePayload.TYPE, VehicleStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VehicleSeatAssignmentsPayload.TYPE, VehicleSeatAssignmentsPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerConfigStatePayload.TYPE, ServerConfigStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DroneControlPayload.TYPE, DroneControlPayload.STREAM_CODEC);

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
        ServerPlayNetworking.registerGlobalReceiver(OpenAttachmentsPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleOpenAttachments(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ToggleMedalsPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleToggleMedals(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(MeleePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleMelee(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(InspectGunPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleInspectGun(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ToggleFlashlightPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleToggleFlashlight(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ChargeFlashlightPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleChargeFlashlight(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(TriggerReleasePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleTriggerRelease(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(AimingStatePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleAimingState(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(GuidedLockPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleGuidedLock(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ToggleLauncherModePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleToggleLauncherMode(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(DroneInputPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleDroneInput(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(VehicleInputPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleVehicleInput(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(VehicleChangeSeatPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleVehicleChangeSeat(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(VehicleDismountPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleVehicleDismount(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(VehicleOpenMenuPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleVehicleOpenMenu(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(AssembleTestVehiclePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleAssembleTestVehicle(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(OpenServerConfigPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleOpenServerConfig(context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(ApplyServerConfigPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> handleApplyServerConfig(payload, context.player()));
        });
    }

    private static void handleOpenServerConfig(ServerPlayer player) {
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                    ServerConfigStatePayload.Status.DENIED, Map.of(), List.of(), 0));
            return;
        }
        ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                ServerConfigStatePayload.Status.OPEN, ServerConfigEditor.snapshot(), List.of(), 0));
    }

    private static void handleApplyServerConfig(ApplyServerConfigPayload payload, ServerPlayer player) {
        if (!player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                    ServerConfigStatePayload.Status.DENIED, Map.of(), List.of(), 0));
            return;
        }
        try {
            ServerConfigEditor.ApplyResult result = ServerConfigEditor.apply(player.level().getServer(), payload.changes());
            ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                    ServerConfigStatePayload.Status.APPLIED, result.values(), List.of(), result.changedCount()));
        } catch (ServerConfigEditor.ValidationException ex) {
            ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                    ServerConfigStatePayload.Status.INVALID, Map.of(), List.of(ex.key()), 0));
        } catch (IllegalArgumentException ex) {
            ServerPlayNetworking.send(player, new ServerConfigStatePayload(
                    ServerConfigStatePayload.Status.INVALID, Map.of(), List.copyOf(payload.changes().keySet()), 0));
        }
    }

    private static void handleVehicleInput(VehicleInputPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            vehicle.processInput(player, payload.toInput());
        }
    }

    private static void handleAssembleTestVehicle(AssembleTestVehiclePayload payload, ServerPlayer player) {
        if (player.containerMenu instanceof VehicleAssemblingMenu menu) {
            menu.assembleVehicle(player, payload.recipeId());
        }
    }

    private static void handleVehicleChangeSeat(VehicleChangeSeatPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            vehicle.changeSeat(player);
        }
    }

    private static void handleVehicleDismount(VehicleDismountPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            vehicle.forgetSeatAssignment(player);
            GunEvents.clearVehicleSeatReturn(player);
            player.stopRiding();
        }
    }

    private static void handleVehicleOpenMenu(VehicleOpenMenuPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            player.openMenu(vehicle);
        }
    }

    private static void handleInspectGun(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof AnimatedGunItem animated) {
            GunItem.cancelReloadForImmediateAction(player, stack);
            animated.triggerInspect(player.level(), player, stack);
        }
    }

    private static void handleShootRequest(ShootRequestPayload payload, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }
        if (GunItem.isDrawOperationLocked(stack)) {
            return;
        }
        if (GunItem.isHoldToFireWeapon(stack) && !hasCompletedHoldFire(player, stack)) {
            return;
        }
        boolean shot = gun.tryShoot(player.level(), player, payload.hand());
        if (shot && GunItem.isHoldToFireWeapon(stack)) {
            HOLD_FIRE_START_TICKS.remove(player.getUUID());
        }
        if (shot && shouldForceExitAdsAfterShot(stack, gun)) {
            AIMING_PLAYERS.remove(player.getUUID());
            player.stopUsingItem();
        }
    }

    private static boolean shouldForceExitAdsAfterShot(ItemStack stack, GunItem gun) {
        return Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.hasTelescopicSight(stack);
    }

    private static void handleHoldFire(HoldFirePayload payload, ServerPlayer player) {
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!payload.holding() || !GunItem.isHoldToFireWeapon(stack)) {
            HOLD_FIRE_START_TICKS.remove(player.getUUID());
            return;
        }
        if (GunItem.isDrawOperationLocked(stack)) {
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
                && !GunItem.isDrawOperationLocked(mainHand)
                && isCoolant(offhand)
                && GunItem.tryStartWaterCooling(player.level(), player, InteractionHand.OFF_HAND)) {
            GunItem.cancelReloadForImmediateAction(player, mainHand);
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
        boolean reloaded = gun.tryReload(player.level(), player, stack, payload.hand(), true);
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

    private static void handleOpenAttachments(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem && !GunItem.isDrawOperationLocked(stack)) {
            GunItem.cancelReloadForImmediateAction(player, stack);
            player.openMenu(AttachmentMenu.provider());
        }
    }

    private static void handleToggleMedals(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem) {
            GunAttachments.toggleMedals(stack);
        }
    }

    private static void handleToggleFlashlight(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem && !GunItem.isDrawOperationLocked(stack)) {
            GunItem.cancelReloadForImmediateAction(player, stack);
            toggleGunFlashlight(player, stack);
        }
    }

    private static void handleMelee(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }
        if (isMeleeBlockedGun(stack) || GunItem.isDrawOperationLocked(stack) || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        GunItem.cancelReloadForImmediateAction(player, stack);
        if (stack.getItem() instanceof AnimatedGunItem animated) {
            animated.triggerMelee(player.level(), player, stack);
        }
        toggleGunFlashlight(player, stack);
        GunAttachments.toggleLaserPointer(stack);
        AttachmentRuntimeEvents.handleBayonetMelee(player);
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

    private static void toggleGunFlashlight(ServerPlayer player, ItemStack stack) {
        if (!GunAttachments.hasFlashlight(stack)) {
            return;
        }
        if (!Config.allowFlashlights()) {
            Component message = Component.translatable("chat.jeg.disabled_flashlights").withStyle(ChatFormatting.GRAY);
            player.sendSystemMessage(message, true);
            return;
        }
        GunAttachments.FlashlightToggleResult result = GunAttachments.toggleFlashlight(stack, player);
        if (result == GunAttachments.FlashlightToggleResult.MISSING) {
            return;
        }
        if (result == GunAttachments.FlashlightToggleResult.DEAD) {
            Component message = Component.translatable("chat.jeg.flashlight_battery_dead").withStyle(ChatFormatting.RED);
            player.sendSystemMessage(message, true);
        }
        var sound = ModSounds.ALL.get(result == GunAttachments.FlashlightToggleResult.DEAD
                ? Reference.id("item.goose")
                : Reference.id("item.flashlight"));
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void handleChargeFlashlight(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof FlashlightAttachmentItem) {
            FlashlightAttachmentItem.charge(stack, player);
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
        if (payload.aiming() && GunItem.isDrawOperationLocked(player.getMainHandItem())) {
            return;
        }
        if (payload.aiming()) {
            GunItem.cancelReloadForImmediateAction(player, player.getMainHandItem());
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

    public static void sendHeadshotMedal(ServerPlayer player) {
        ServerPlayNetworking.send(player, HeadshotMedalPayload.INSTANCE);
    }

    public static void sendMedal(ServerPlayer player, MedalType medal) {
        ServerPlayNetworking.send(player, new MedalPayload(medal));
    }

    public static void sendKillMedal(ServerPlayer player) {
        ServerPlayNetworking.send(player, KillMedalPayload.INSTANCE);
    }

    public static void sendUiConfig(ServerPlayer player) {
        ServerPlayNetworking.send(player, new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback(), Config.hideMedals()));
    }

    public static void sendVehicleData(ServerPlayer player) {
        ServerPlayNetworking.send(player, new VehicleDataSyncPayload(VehicleDataManager.syncedJson()));
        ServerPlayNetworking.send(player, new VehicleAssemblyRecipeSyncPayload(VehicleAssemblyRecipeManager.syncedJson()));
    }

    public static void broadcastUiConfig(MinecraftServer server) {
        UiConfigPayload payload = new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback(), Config.hideMedals());
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void syncVehicleState(ServerPlayer player, VehicleEntity vehicle, boolean forceApply) {
        ServerPlayNetworking.send(player, new VehicleStatePayload(
                vehicle.getId(),
                vehicle.getX(),
                vehicle.getY(),
                vehicle.getZ(),
                vehicle.getDeltaMovement().x,
                vehicle.getDeltaMovement().y,
                vehicle.getDeltaMovement().z,
                vehicle.getYRot(),
                vehicle.getXRot(),
                forceApply
        ));
    }

    private static void syncVehicleStateToTrackingPlayers(VehicleEntity vehicle, boolean forceApply) {
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        // Tracking full + 256-block fallback (deduped).
        HashSet<ServerPlayer> recipients = new HashSet<>();
        for (ServerPlayer player : PlayerLookup.tracking(vehicle)) {
            recipients.add(player);
        }
        for (ServerPlayer player : PlayerLookup.level(level)) {
            if (player.distanceToSqr(vehicle) <= VEHICLE_STATE_FALLBACK_DISTANCE_SQR) {
                recipients.add(player);
            }
        }
        for (ServerPlayer player : recipients) {
            syncVehicleState(player, vehicle, forceApply);
        }
    }

    public static void sendVehicleState(ServerPlayer player, VehicleEntity vehicle) {
        syncVehicleState(player, vehicle, false);
    }

    public static void broadcastVehicleState(VehicleEntity vehicle) {
        syncVehicleStateToTrackingPlayers(vehicle, false);
    }

    public static void sendForcedVehicleState(ServerPlayer player, VehicleEntity vehicle) {
        syncVehicleState(player, vehicle, true);
    }

    public static void broadcastForcedVehicleState(VehicleEntity vehicle) {
        syncVehicleStateToTrackingPlayers(vehicle, true);
    }

    public static void broadcastVehicleSeatAssignments(VehicleEntity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        VehicleSeatAssignmentsPayload payload = VehicleSeatAssignmentsPayload.fromMap(vehicle.getId(), vehicle.seatAssignmentsSnapshot());
        for (ServerPlayer player : PlayerLookup.level(level)) {
            ServerPlayNetworking.send(player, payload);
        }
    }


    private static void handleGuidedLock(GuidedLockPayload payload, ServerPlayer player) {
        GuidedLauncherItem.updateLock(player, payload.hand(), payload.targetId());
    }

    private static void handleToggleLauncherMode(ServerPlayer player) {
        GuidedLauncherItem.toggleMode(player, player.getMainHandItem());
    }

    private static void handleDroneInput(DroneInputPayload payload, ServerPlayer player) {
        if (player.level().getEntity(payload.entityId()) instanceof DroneEntity drone) {
            drone.processInput(player, payload.inputs(), payload.yawDelta(), payload.pitchDelta());
        }
    }

    public static void sendDroneControl(ServerPlayer player, int entityId, boolean active, int maxRange) {
        ServerPlayNetworking.send(player, new DroneControlPayload(entityId, active, maxRange));
    }

}
