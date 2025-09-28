package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.common.network.ServerPlayHandler;

public record C2SMessageBurst()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageBurst> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageBurst()
    );

    public static void handle(C2SMessageBurst message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(ServerPlayHandler::handleBurst));
        context.setHandled(true);
    }
}
