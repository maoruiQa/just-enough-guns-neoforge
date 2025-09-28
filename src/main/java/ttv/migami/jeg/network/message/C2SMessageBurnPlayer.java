package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageBurnPlayer()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageBurnPlayer> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageBurnPlayer()
    );

    public static void handle(C2SMessageBurnPlayer message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(player -> {
            if(!player.isSpectator())
            {
                ServerPlayHandler.burnPlayer(player);
            }
        }));
        context.setHandled(true);
    }
}
