package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.medal.MedalManager;

/**
 * Author: MrCrayfish
 */
public record S2CMessageSendHeadshot(boolean headshot)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageSendHeadshot> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.headshot),
            buffer -> new S2CMessageSendHeadshot(buffer.readBoolean())
    );

    public static void handle(S2CMessageSendHeadshot message, PlayMessageContext context)
    {
        context.execute(() -> MedalManager.setHeadshot(true));
        context.setHandled(true);
    }
}
