package ttv.migami.jeg.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.client.ClientUiConfig;
import ttv.migami.jeg.client.CrosshairHandler;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.ServerConfigClient;
import ttv.migami.jeg.client.medal.MedalManager;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;
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

public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}

    private static boolean clientRegistered;

    public static void initClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        ClientPlayNetworking.registerGlobalReceiver(BulletTrailPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (!NetworkHandler.shouldRenderLegacyBulletTrail()) {
                    return;
                }
                BulletTrailRenderer.upsertLegacyTrail(payload);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GunFireFxPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> FabricClientBootstrap.showMuzzleFlash(payload.shooterId(), payload.randomValue()));
        });

        ClientPlayNetworking.registerGlobalReceiver(OffhandFullPromptPayload.TYPE, (payload, context) -> {
            context.client().execute(FabricClientBootstrap::showOffhandFullPrompt);
        });

        ClientPlayNetworking.registerGlobalReceiver(HitMarkerPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> CrosshairHandler.playHitMarker(payload.critical()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HeadshotMedalPayload.TYPE, (payload, context) -> {
            context.client().execute(MedalManager::showHeadshot);
        });

        ClientPlayNetworking.registerGlobalReceiver(MedalPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> MedalManager.showMedal(payload.medal().ordinal()));
        });

        ClientPlayNetworking.registerGlobalReceiver(KillMedalPayload.TYPE, (payload, context) -> {
            context.client().execute(MedalManager::showKill);
        });

        ClientPlayNetworking.registerGlobalReceiver(UiConfigPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientUiConfig.update(payload.showCrosshair(), payload.showHitFeedback(), payload.hideMedals()));
        });

        ClientPlayNetworking.registerGlobalReceiver(ServerConfigStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ServerConfigClient.handle(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(VehicleDataSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> VehicleDataManager.applySyncedJson(payload.data()));
        });

        ClientPlayNetworking.registerGlobalReceiver(VehicleAssemblyRecipeSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> VehicleAssemblyRecipeManager.applySyncedJson(payload.recipes()));
        });

        ClientPlayNetworking.registerGlobalReceiver(VehicleStatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level == null) {
                    return;
                }
                var entity = context.client().level.getEntity(payload.vehicleId());
                if (entity instanceof VehicleEntity vehicle) {
                    vehicle.syncAuthoritativeState(payload.x(), payload.y(), payload.z(), payload.motionX(), payload.motionY(), payload.motionZ(), payload.yaw(), payload.pitch(), payload.forceApply());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(VehicleSeatAssignmentsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().level == null) {
                    return;
                }
                var entity = context.client().level.getEntity(payload.vehicleId());
                if (entity instanceof VehicleEntity vehicle) {
                    vehicle.applySeatAssignments(payload.toMap());
                }
            });
        });
    }

    public static void sendTriggerRelease(InteractionHand hand) {
        ClientPlayNetworking.send(new TriggerReleasePayload(hand));
    }

    public static void sendShoot(InteractionHand hand) {
        ClientPlayNetworking.send(new ShootRequestPayload(hand));
    }

    public static void sendHoldFire(InteractionHand hand, boolean holding) {
        ClientPlayNetworking.send(new HoldFirePayload(hand, holding));
    }

    public static void sendReload(InteractionHand hand) {
        ClientPlayNetworking.send(new ReloadRequestPayload(hand));
    }

    public static void sendUnloadMagazine() {
        ClientPlayNetworking.send(UnloadMagazineRequestPayload.INSTANCE);
    }

    public static void sendOpenAttachments() {
        ClientPlayNetworking.send(OpenAttachmentsPayload.INSTANCE);
    }

    public static void sendToggleMedals() {
        ClientPlayNetworking.send(ToggleMedalsPayload.INSTANCE);
    }

    public static void sendMelee() {
        ClientPlayNetworking.send(MeleePayload.INSTANCE);
    }

    public static void sendInspect() {
        ClientPlayNetworking.send(InspectGunPayload.INSTANCE);
    }

    public static void sendToggleFlashlight() {
        ClientPlayNetworking.send(ToggleFlashlightPayload.INSTANCE);
    }

    public static void sendChargeFlashlight() {
        ClientPlayNetworking.send(ChargeFlashlightPayload.INSTANCE);
    }

    public static void sendAiming(boolean aiming) {
        ClientPlayNetworking.send(new AimingStatePayload(aiming));
    }

    public static void sendOpenServerConfig() {
        ClientPlayNetworking.send(OpenServerConfigPayload.INSTANCE);
    }

    public static void sendServerConfigChanges(ApplyServerConfigPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendVehicleInput(int vehicleId, VehicleInput input) {
        ClientPlayNetworking.send(new VehicleInputPayload(
                vehicleId,
                input.forward(),
                input.backward(),
                input.left(),
                input.right(),
                input.brake(),
                input.ascend(),
                input.descend(),
                input.fire(),
                input.reload(),
                input.freeLook(),
                input.switchWeapon(),
                input.previousWeapon(),
                input.weaponSlot(),
                input.seekTarget(),
                input.deployDecoy(),
                input.mouseX(),
                input.mouseY()
        ));
    }

    public static void sendVehicleOpenMenu(int vehicleId) {
        ClientPlayNetworking.send(new VehicleOpenMenuPayload(vehicleId));
    }

    public static void sendAssembleVehicle(Identifier recipeId) {
        ClientPlayNetworking.send(new AssembleTestVehiclePayload(recipeId));
    }

    public static void sendVehicleChangeSeat(int vehicleId) {
        ClientPlayNetworking.send(new VehicleChangeSeatPayload(vehicleId));
    }

    public static void sendVehicleDismount(int vehicleId) {
        ClientPlayNetworking.send(new VehicleDismountPayload(vehicleId));
    }
}
