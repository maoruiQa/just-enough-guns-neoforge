package ttv.migami.jeg.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.compat.ClientHooks;

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
                if (!payload.trailVisible()) {
                    return;
                }
                int count = Math.min(payload.positions().length, payload.motions().length);
                for (int i = 0; i < count; i++) {
                    var start = payload.positions()[i];
                    var end = start.add(payload.motions()[i]);
                    ClientHooks.addBulletTrail(start, end, payload.color(), payload.size());
                }
            });
        });
    }

    public static void sendTriggerRelease(InteractionHand hand) {
        ClientPlayNetworking.send(new TriggerReleasePayload(hand));
    }
}
