package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.medal.MedalManager;

/**
 * Author: MrCrayfish
 */
public record S2CMessageSendMedal(int medal)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageSendMedal> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeInt(message.medal),
            buffer -> new S2CMessageSendMedal(buffer.readInt())
    );

    public static void handle(S2CMessageSendMedal message, PlayMessageContext context)
    {
        context.execute(() -> MedalManager.addEnumMedal(message.medal));
        context.setHandled(true);
    }
}
