package ttv.migami.jeg.network;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;

public final class NetworkHandler {
    private NetworkHandler() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Reference.MOD_ID)
                .playToServer(TriggerReleasePayload.TYPE, TriggerReleasePayload.STREAM_CODEC, NetworkHandler::handleTriggerRelease);
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

    public static void sendTriggerRelease(InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new TriggerReleasePayload(hand));
    }
}
