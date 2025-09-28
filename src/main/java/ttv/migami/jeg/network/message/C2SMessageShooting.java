package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.init.ModSyncedDataKeys;

/**
 * Author: MrCrayfish
 */
public record C2SMessageShooting(boolean shooting)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageShooting> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.shooting),
            buffer -> new C2SMessageShooting(buffer.readBoolean())
    );

    public static void handle(C2SMessageShooting message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null)
            {
                ModSyncedDataKeys.SHOOTING.setValue(player, message.shooting);
            }
        });
        context.setHandled(true);
    }
}
