package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageFirstPersonReload(boolean firstPerson)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageFirstPersonReload> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.firstPerson),
            buffer -> new C2SMessageFirstPersonReload(buffer.readBoolean())
    );

    public static void handle(C2SMessageFirstPersonReload message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null)
            {
                ServerPlayHandler.handleReloadPerspective(player, message.firstPerson);
            }
        });
        context.setHandled(true);
    }
}
