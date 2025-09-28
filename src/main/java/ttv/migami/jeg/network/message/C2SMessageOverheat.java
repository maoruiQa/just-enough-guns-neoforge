package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageOverheat()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageOverheat> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageOverheat()
    );

    public static void handle(C2SMessageOverheat message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.overheat(player);
            }
        });
        context.setHandled(true);
    }
}
