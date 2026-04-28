package ttv.migami.jeg.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.client.ClientUiConfig;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.network.NetworkHandler;

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
            context.client().execute(() -> ttv.migami.jeg.client.CrosshairHandler.playHitMarker(payload.critical()));
        });

        ClientPlayNetworking.registerGlobalReceiver(UiConfigPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientUiConfig.update(payload.showCrosshair(), payload.showHitFeedback()));
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

    public static void sendAiming(boolean aiming) {
        ClientPlayNetworking.send(new AimingStatePayload(aiming));
    }
}
