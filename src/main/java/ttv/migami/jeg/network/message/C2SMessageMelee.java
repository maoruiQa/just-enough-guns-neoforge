package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageMelee()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageMelee> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageMelee()
    );

    public static void handle(C2SMessageMelee message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.handleMelee(player);
            }
        });
        context.setHandled(true);
    }
}
