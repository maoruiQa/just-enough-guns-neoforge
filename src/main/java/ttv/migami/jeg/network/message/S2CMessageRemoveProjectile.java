package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 */
public record S2CMessageRemoveProjectile(int entityId)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageRemoveProjectile> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeInt(message.entityId),
            buffer -> new S2CMessageRemoveProjectile(buffer.readInt())
    );

    public static void handle(S2CMessageRemoveProjectile message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleRemoveProjectile(message));
        context.setHandled(true);
    }

    public int getEntityId()
    {
        return this.entityId;
    }
}
