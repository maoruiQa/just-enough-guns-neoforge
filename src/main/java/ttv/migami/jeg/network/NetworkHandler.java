package ttv.migami.jeg.network;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.event.AttachmentRuntimeEvents;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
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
    private static final String AIMING_TAG = "jeg_aiming";
    private static final Map<UUID, Long> HOLD_FIRE_START_TICKS = new HashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Reference.MOD_ID)
                .playToServer(ShootRequestPayload.TYPE, ShootRequestPayload.STREAM_CODEC, NetworkHandler::handleShootRequest)
                .playToServer(HoldFirePayload.TYPE, HoldFirePayload.STREAM_CODEC, NetworkHandler::handleHoldFire)
                .playToServer(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC, NetworkHandler::handleTriggerRelease)
                .playToServer(ReloadRequestPayload.TYPE, ReloadRequestPayload.STREAM_CODEC, NetworkHandler::handleReloadRequest)
                .playToServer(UnloadMagazineRequestPayload.TYPE, UnloadMagazineRequestPayload.STREAM_CODEC, NetworkHandler::handleUnloadMagazineRequest)
                .playToServer(OpenAttachmentsPayload.TYPE, OpenAttachmentsPayload.STREAM_CODEC, NetworkHandler::handleOpenAttachments)
                .playToServer(MeleePayload.TYPE, MeleePayload.STREAM_CODEC, NetworkHandler::handleMelee)
                .playToServer(InspectGunPayload.TYPE, InspectGunPayload.STREAM_CODEC, NetworkHandler::handleInspectGun)
                .playToServer(ToggleFlashlightPayload.TYPE, ToggleFlashlightPayload.STREAM_CODEC, NetworkHandler::handleToggleFlashlight)
                .playToServer(ChargeFlashlightPayload.TYPE, ChargeFlashlightPayload.STREAM_CODEC, NetworkHandler::handleChargeFlashlight)
                .playToServer(ToggleMedalsPayload.TYPE, ToggleMedalsPayload.STREAM_CODEC, NetworkHandler::handleToggleMedals)
                .playToServer(AimingStatePayload.TYPE, AimingStatePayload.STREAM_CODEC, NetworkHandler::handleAimingState)
                .playToServer(VehicleInputPayload.TYPE, VehicleInputPayload.STREAM_CODEC, NetworkHandler::handleVehicleInput)
                .playToServer(VehicleChangeSeatPayload.TYPE, VehicleChangeSeatPayload.STREAM_CODEC, NetworkHandler::handleVehicleChangeSeat)
                .playToServer(VehicleDismountPayload.TYPE, VehicleDismountPayload.STREAM_CODEC, NetworkHandler::handleVehicleDismount)
                .playToServer(VehicleOpenMenuPayload.TYPE, VehicleOpenMenuPayload.STREAM_CODEC, NetworkHandler::handleVehicleOpenMenu)
                .playToServer(AssembleTestVehiclePayload.TYPE, AssembleTestVehiclePayload.STREAM_CODEC, NetworkHandler::handleAssembleTestVehicle)
                .playToClient(BulletTrailPayload.TYPE, BulletTrailPayload.STREAM_CODEC, NetworkHandler::handleBulletTrail)
                .playToClient(GunFireFxPayload.TYPE, GunFireFxPayload.STREAM_CODEC, NetworkHandler::handleGunFireFx)
                .playToClient(OffhandFullPromptPayload.TYPE, OffhandFullPromptPayload.STREAM_CODEC, NetworkHandler::handleOffhandFullPrompt)
                .playToClient(UiConfigPayload.TYPE, UiConfigPayload.STREAM_CODEC, NetworkHandler::handleUiConfig)
                .playToClient(VehicleDataSyncPayload.TYPE, VehicleDataSyncPayload.STREAM_CODEC, NetworkHandler::handleVehicleDataSync)
                .playToClient(VehicleAssemblyRecipeSyncPayload.TYPE, VehicleAssemblyRecipeSyncPayload.STREAM_CODEC, NetworkHandler::handleVehicleAssemblyRecipeSync)
                .playToClient(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC, NetworkHandler::handleHitMarker)
                .playToClient(HeadshotMedalPayload.TYPE, HeadshotMedalPayload.STREAM_CODEC, NetworkHandler::handleHeadshotMedal)
                .playToClient(MedalPayload.TYPE, MedalPayload.STREAM_CODEC, NetworkHandler::handleMedal)
                .playToClient(KillMedalPayload.TYPE, KillMedalPayload.STREAM_CODEC, NetworkHandler::handleKillMedal)
                .playToClient(VehicleStatePayload.TYPE, VehicleStatePayload.STREAM_CODEC, NetworkHandler::handleVehicleState)
                .playToClient(VehicleSeatAssignmentsPayload.TYPE, VehicleSeatAssignmentsPayload.STREAM_CODEC, NetworkHandler::handleVehicleSeatAssignments);
    }

    private static void handleOpenAttachments(OpenAttachmentsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getMainHandItem().getItem() instanceof GunItem
                    && !GunItem.isDrawing(player.getMainHandItem())) {
                player.openMenu(AttachmentMenu.provider());
            }
        });
    }

    private static void handleToggleFlashlight(ToggleFlashlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem && !GunItem.isDrawing(stack)) {
                toggleGunFlashlight(player, stack);
            }
        });
    }

    private static void handleMelee(MeleePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) {
                return;
            }
            if (isMeleeBlockedGun(stack) || GunItem.isDrawing(stack) || player.getCooldowns().isOnCooldown(stack)) {
                return;
            }
            GunItem.cancelReloadForImmediateAction(player, stack);
            if (stack.getItem() instanceof AnimatedGunItem animated) {
                animated.triggerMelee(player.level(), player, stack);
            }
            toggleGunFlashlight(player, stack);
            GunAttachments.toggleLaserPointer(stack);
            AttachmentRuntimeEvents.handleBayonetMelee(player);
        });
    }

    private static void handleInspectGun(InspectGunPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof AnimatedGunItem animated) {
                animated.triggerInspect(player.level(), player, stack);
            }
        });
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
        Identifier soundId = result == GunAttachments.FlashlightToggleResult.DEAD
                ? Reference.id("item.goose")
                : Reference.id("item.flashlight");
        var sound = ModSounds.ALL.get(soundId);
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void handleChargeFlashlight(ChargeFlashlightPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof FlashlightAttachmentItem) {
                FlashlightAttachmentItem.charge(stack, player);
            }
        });
    }

    private static void handleToggleMedals(ToggleMedalsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof GunItem) {
                GunAttachments.toggleMedals(stack);
            }
        });
    }

    private static void handleVehicleInput(VehicleInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.getVehicle() instanceof VehicleEntity vehicle) || vehicle.getId() != payload.vehicleId()) {
                return;
            }
            vehicle.processInput(player, payload.toInput());
        });
    }

    private static void handleAssembleTestVehicle(AssembleTestVehiclePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VehicleAssemblingMenu menu) {
                menu.assembleVehicle(player, payload.recipeId());
            }
        });
    }

    private static void handleVehicleChangeSeat(VehicleChangeSeatPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
                vehicle.changeSeat(player);
            }
        });
    }

    private static void handleVehicleDismount(VehicleDismountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
                GunEvents.clearVehicleSeatReturn(player);
                vehicle.forgetSeatAssignment(player);
                player.stopRiding();
            }
        });
    }

    private static void handleVehicleOpenMenu(VehicleOpenMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
                player.openMenu(vehicle, buffer -> buffer.writeVarInt(vehicle.vehicleContainerSlots()));
            }
        });
    }

    private static void handleBulletTrail(BulletTrailPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!Config.legacyBulletTrailEnabled()) {
                return;
            }
            invokeClientStatic(
                    "ttv.migami.jeg.client.render.BulletTrailRenderer",
                    "upsertLegacyTrail",
                    new Class<?>[] { BulletTrailPayload.class },
                    payload);
        });
    }

    private static void handleAimingState(AimingStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.aiming() && GunItem.isDrawing(player.getMainHandItem())) {
                    return;
                }
                player.getPersistentData().putBoolean(AIMING_TAG, payload.aiming());
            }
        });
    }

    private static void handleGunFireFx(GunFireFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.GunClientEvents",
                "showMuzzleFlash",
                new Class<?>[] { int.class, float.class },
                payload.shooterId(),
                payload.randomValue()));
    }

    private static void handleOffhandFullPrompt(OffhandFullPromptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.GunClientEvents",
                "showOffhandFullPrompt",
                new Class<?>[0]));
    }

    private static void handleHitMarker(HitMarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.CrosshairHandler",
                "playHitMarker",
                new Class<?>[] { boolean.class },
                payload.critical()));
    }

    private static void handleHeadshotMedal(HeadshotMedalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.medal.MedalManager",
                "showHeadshot",
                new Class<?>[0]));
    }

    private static void handleMedal(MedalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.medal.MedalManager",
                "showMedal",
                new Class<?>[] { int.class },
                payload.medal().ordinal()));
    }

    private static void handleKillMedal(KillMedalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.medal.MedalManager",
                "showKill",
                new Class<?>[0]));
    }

    private static void handleUiConfig(UiConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> invokeClientStatic(
                "ttv.migami.jeg.client.ClientUiConfig",
                "update",
                new Class<?>[] { boolean.class, boolean.class, boolean.class },
                payload.showCrosshair(),
                payload.showHitFeedback(),
                payload.hideMedals()));
    }

    private static void handleVehicleDataSync(VehicleDataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VehicleDataManager.applySyncedJson(payload.data()));
    }

    private static void handleVehicleAssemblyRecipeSync(VehicleAssemblyRecipeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> VehicleAssemblyRecipeManager.applySyncedJson(payload.recipes()));
    }

    private static void syncVehicleState(ServerPlayer player, VehicleEntity vehicle, boolean forceApply) {
        player.connection.send(new VehicleStatePayload(
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
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(vehicle) <= 4096.0D) {
                syncVehicleState(player, vehicle, forceApply);
            }
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
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) {
                player.connection.send(payload);
            }
        }
    }

    private static void handleVehicleState(VehicleStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(clientLevel() instanceof Level level)) {
                return;
            }
            var entity = level.getEntity(payload.vehicleId());
            if (!(entity instanceof VehicleEntity vehicle)) {
                return;
            }
            vehicle.syncAuthoritativeState(payload.x(), payload.y(), payload.z(), payload.motionX(), payload.motionY(), payload.motionZ(), payload.yaw(), payload.pitch(), payload.forceApply());
        });
    }

    private static void handleVehicleSeatAssignments(VehicleSeatAssignmentsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(clientLevel() instanceof Level level)) {
                return;
            }
            var entity = level.getEntity(payload.vehicleId());
            if (!(entity instanceof VehicleEntity vehicle)) {
                return;
            }
            vehicle.applySeatAssignments(payload.toMap());
        });
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
            if (GunItem.isDrawing(stack)) {
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
            if (GunItem.isDrawing(stack)) {
                return;
            }
            player.getPersistentData().putBoolean(AIMING_TAG, payload.aiming());

            if (GunItem.isHoldToFireWeapon(stack) && !hasCompletedHoldFire(player, stack)) {
                return;
            }

            boolean shot = gun.tryShoot(player.level(), player, payload.hand());
            if (shot && GunItem.isHoldToFireWeapon(stack)) {
                HOLD_FIRE_START_TICKS.remove(player.getUUID());
            }
            if (shot && shouldForceExitAdsAfterShot(stack, gun)) {
                player.getPersistentData().putBoolean(AIMING_TAG, false);
                player.stopUsingItem();
            }
        });
    }

    private static boolean shouldForceExitAdsAfterShot(ItemStack stack, GunItem gun) {
        return Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.hasTelescopicSight(stack);
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
                    && !GunItem.isDrawing(mainHand)
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
            boolean reloaded = gun.tryReload(player.level(), player, stack, payload.hand(), true);
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
        sendToServer(new TriggerReleasePayload(hand));
    }

    public static void sendReload(InteractionHand hand) {
        sendToServer(new ReloadRequestPayload(hand));
    }

    public static void sendShoot(InteractionHand hand) {
        sendToServer(new ShootRequestPayload(hand, clientAiming()));
    }

    public static void sendHoldFire(InteractionHand hand, boolean holding) {
        sendToServer(new HoldFirePayload(hand, holding));
    }

    public static void sendAiming(boolean aiming) {
        sendToServer(new AimingStatePayload(aiming));
    }

    public static void sendUnloadMagazine() {
        sendToServer(UnloadMagazineRequestPayload.INSTANCE);
    }

    public static void sendOpenAttachments() {
        sendToServer(OpenAttachmentsPayload.INSTANCE);
    }

    public static void sendMelee() {
        sendToServer(MeleePayload.INSTANCE);
    }

    public static void sendInspect() {
        sendToServer(InspectGunPayload.INSTANCE);
    }

    public static void sendToggleFlashlight() {
        sendToServer(ToggleFlashlightPayload.INSTANCE);
    }

    public static void sendChargeFlashlight() {
        sendToServer(ChargeFlashlightPayload.INSTANCE);
    }

    public static void sendToggleMedals() {
        sendToServer(ToggleMedalsPayload.INSTANCE);
    }

    public static void sendAssembleVehicle(Identifier recipeId) {
        sendToServer(new AssembleTestVehiclePayload(recipeId));
    }

    public static void sendVehicleChangeSeat(int vehicleId) {
        sendToServer(new VehicleChangeSeatPayload(vehicleId));
    }

    public static void sendVehicleDismount(int vehicleId) {
        sendToServer(new VehicleDismountPayload(vehicleId));
    }

    public static void sendVehicleOpenMenu(int vehicleId) {
        sendToServer(new VehicleOpenMenuPayload(vehicleId));
    }

    private static Object clientLevel() {
        Object client = minecraftInstance();
        if (client == null) {
            return null;
        }
        try {
            return client.getClass().getField("level").get(client);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean clientAiming() {
        try {
            Class<?> handlerClass = Class.forName("ttv.migami.jeg.client.handler.AimingHandler");
            Object handler = handlerClass.getMethod("get").invoke(null);
            Object aiming = handlerClass.getMethod("isAiming").invoke(handler);
            return aiming instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static void sendToServer(Object payload) {
        Object client = minecraftInstance();
        if (client == null) {
            return;
        }
        try {
            Object connection = client.getClass().getMethod("getConnection").invoke(client);
            if (connection == null) {
                return;
            }
            for (Method method : connection.getClass().getMethods()) {
                if (!"send".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (method.getParameterTypes()[0].isAssignableFrom(payload.getClass())) {
                    method.invoke(connection, payload);
                    return;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Object minecraftInstance() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            return minecraftClass.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void invokeClientStatic(String className, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> handlerClass = Class.forName(className);
            handlerClass.getMethod(methodName, parameterTypes).invoke(null, args);
        } catch (ReflectiveOperationException ignored) {
        }
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

    public static void sendHeadshotMedal(ServerPlayer player) {
        player.connection.send(HeadshotMedalPayload.INSTANCE);
    }

    public static void sendMedal(ServerPlayer player, MedalType medal) {
        player.connection.send(new MedalPayload(medal));
    }

    public static void sendKillMedal(ServerPlayer player) {
        player.connection.send(KillMedalPayload.INSTANCE);
    }

    public static void sendUiConfig(ServerPlayer player) {
        player.connection.send(new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback(), Config.hideMedals()));
    }

    public static void sendVehicleData(ServerPlayer player) {
        player.connection.send(new VehicleDataSyncPayload(VehicleDataManager.syncedJson()));
        player.connection.send(new VehicleAssemblyRecipeSyncPayload(VehicleAssemblyRecipeManager.syncedJson()));
    }

    public static void broadcastUiConfig(MinecraftServer server) {
        UiConfigPayload payload = new UiConfigPayload(Config.showCrosshair(), Config.showHitFeedback(), Config.hideMedals());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(payload);
        }
    }

    public static boolean isAiming(Player player) {
        return player.getPersistentData().getBoolean(AIMING_TAG).orElse(false);
    }
}
