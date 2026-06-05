package ttv.migami.jeg.network;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.event.AttachmentRuntimeEvents;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.attachment.GunAttachments;
import ttv.migami.jeg.menu.AttachmentMenu;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
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

    private static boolean commonRegistered;
    private static final Set<UUID> AIMING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> HOLD_FIRE_START_TICKS = new HashMap<>();

    public static void initCommon() {
        if (commonRegistered) {
            return;
        }
        commonRegistered = true;

        PayloadTypeRegistry.playC2S().register(ShootRequestPayload.TYPE, ShootRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(HoldFirePayload.TYPE, HoldFirePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ReloadRequestPayload.TYPE, ReloadRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UnloadMagazineRequestPayload.TYPE, UnloadMagazineRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(OpenAttachmentsPayload.TYPE, OpenAttachmentsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleMedalsPayload.TYPE, ToggleMedalsPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(MeleePayload.TYPE, MeleePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ToggleFlashlightPayload.TYPE, ToggleFlashlightPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ChargeFlashlightPayload.TYPE, ChargeFlashlightPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AimingStatePayload.TYPE, AimingStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(VehicleInputPayload.TYPE, VehicleInputPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(VehicleChangeSeatPayload.TYPE, VehicleChangeSeatPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(VehicleDismountPayload.TYPE, VehicleDismountPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(VehicleOpenMenuPayload.TYPE, VehicleOpenMenuPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AssembleTestVehiclePayload.TYPE, AssembleTestVehiclePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(BulletTrailPayload.TYPE, BulletTrailPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(GunFireFxPayload.TYPE, GunFireFxPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OffhandFullPromptPayload.TYPE, OffhandFullPromptPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HitMarkerPayload.TYPE, HitMarkerPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(HeadshotMedalPayload.TYPE, HeadshotMedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MedalPayload.TYPE, MedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(KillMedalPayload.TYPE, KillMedalPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(UiConfigPayload.TYPE, UiConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleDataSyncPayload.TYPE, VehicleDataSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleAssemblyRecipeSyncPayload.TYPE, VehicleAssemblyRecipeSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleStatePayload.TYPE, VehicleStatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(VehicleSeatAssignmentsPayload.TYPE, VehicleSeatAssignmentsPayload.STREAM_CODEC);

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
    }

    private static void handleVehicleInput(VehicleInputPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            vehicle.processInput(player, payload.toInput());
        }
    }

    private static void handleVehicleChangeSeat(VehicleChangeSeatPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            vehicle.changeSeat(player);
        }
    }

    private static void handleVehicleDismount(VehicleDismountPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            ttv.migami.jeg.event.GunEvents.clearVehicleSeatReturn(player);
            vehicle.forgetSeatAssignment(player);
            player.stopRiding();
        }
    }

    private static void handleVehicleOpenMenu(VehicleOpenMenuPayload payload, ServerPlayer player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.getId() == payload.vehicleId()) {
            player.openMenu(vehicle);
        }
    }

    private static void handleAssembleTestVehicle(AssembleTestVehiclePayload payload, ServerPlayer player) {
        if (player.containerMenu instanceof VehicleAssemblingMenu menu) {
            menu.assembleVehicle(player, payload.recipeId());
        }
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
        if (shot && shouldForceExitAdsAfterShot(gun)) {
            AIMING_PLAYERS.remove(player.getUUID());
            player.stopUsingItem();
        }
    }

    private static boolean shouldForceExitAdsAfterShot(GunItem gun) {
        return Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled();
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
            player.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private static void handleOpenAttachments(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof GunItem) {
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
        if (stack.getItem() instanceof GunItem) {
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
        if (isMeleeBlockedGun(stack) || GunItem.isDrawing(stack) || player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }
        GunItem.cancelReloadForImmediateAction(player, stack);
        if (stack.getItem() instanceof AnimatedGunItem animated) {
            animated.triggerMelee(player.level(), player, stack);
        }
        toggleGunFlashlight(player, stack);
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
            player.displayClientMessage(message, true);
            return;
        }
        GunAttachments.FlashlightToggleResult result = GunAttachments.toggleFlashlight(stack, player);
        if (result == GunAttachments.FlashlightToggleResult.MISSING) {
            return;
        }
        if (result == GunAttachments.FlashlightToggleResult.DEAD) {
            Component message = Component.translatable("chat.jeg.flashlight_battery_dead").withStyle(ChatFormatting.RED);
            player.displayClientMessage(message, true);
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
        for (ServerPlayer player : PlayerLookup.world(level)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendBulletTrail(ServerPlayer player, BulletTrailPayload payload) {
        ServerPlayNetworking.send(player, payload);
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

    public static void sendVehicleState(ServerPlayer player, VehicleEntity vehicle) {
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
                false
        ));
    }

    public static void broadcastVehicleState(VehicleEntity vehicle) {
        broadcastVehicleState(vehicle, false);
    }

    public static void broadcastForcedVehicleState(VehicleEntity vehicle) {
        broadcastVehicleState(vehicle, true);
    }

    private static void broadcastVehicleState(VehicleEntity vehicle, boolean forceApply) {
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        VehicleStatePayload payload = new VehicleStatePayload(
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
        );
        for (ServerPlayer player : PlayerLookup.world(level)) {
            if (player.distanceToSqr(vehicle) <= 4096.0D) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void broadcastVehicleSeatAssignments(VehicleEntity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        VehicleSeatAssignmentsPayload payload = VehicleSeatAssignmentsPayload.fromMap(vehicle.getId(), vehicle.seatAssignmentsSnapshot());
        for (ServerPlayer player : PlayerLookup.world(level)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
