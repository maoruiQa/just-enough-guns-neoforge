package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageAttachments()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageAttachments> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageAttachments()
    );

    public static void handle(C2SMessageAttachments message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(ServerPlayHandler::handleAttachments));
        context.setHandled(true);
    }
}
