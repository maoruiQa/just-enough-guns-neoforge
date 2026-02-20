package ttv.migami.jeg.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.compat.ClientHooks;

public final class ClientNetworkHandler {
    private ClientNetworkHandler() {}

    private static boolean clientRegistered;
    private static final double LOCAL_FLASH_GUARD_DISTANCE_SQR = 3.0D * 3.0D;

    public static void initClient() {
        if (clientRegistered) {
            return;
        }
        clientRegistered = true;

        ClientPlayNetworking.registerGlobalReceiver(BulletTrailPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (!payload.trailVisible()) {
                    return;
                }

                boolean localShooter = context.client().player != null && payload.shooterId() == context.client().player.getId();
                Vec3 cameraPos = context.client().gameRenderer.getMainCamera().getPosition();
                int count = Math.min(payload.positions().length, payload.motions().length);
                for (int i = 0; i < count; i++) {
                    var start = payload.positions()[i];
                    if (localShooter && cameraPos.distanceToSqr(start) <= LOCAL_FLASH_GUARD_DISTANCE_SQR) {
                        continue;
                    }
                    var end = start.add(payload.motions()[i]);
                    ClientHooks.addBulletTrail(start, end, payload.color(), payload.size());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(GunFireFxPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> FabricClientBootstrap.showMuzzleFlash(payload.shooterId(), payload.randomValue()));
        });
    }

    public static void sendTriggerRelease(InteractionHand hand) {
        ClientPlayNetworking.send(new TriggerReleasePayload(hand));
    }

    public static void sendShoot(InteractionHand hand) {
        ClientPlayNetworking.send(new ShootRequestPayload(hand));
    }

    public static void sendReload(InteractionHand hand) {
        ClientPlayNetworking.send(new ReloadRequestPayload(hand));
    }

    public static void sendAiming(boolean aiming) {
        ClientPlayNetworking.send(new AimingStatePayload(aiming));
    }
}
