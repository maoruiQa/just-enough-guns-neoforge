package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.network.ClientPlayHandler;

public record S2CMessageStunGrenade(double x, double y, double z)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageStunGrenade> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeDouble(message.x);
                buffer.writeDouble(message.y);
                buffer.writeDouble(message.z);
            },
            buffer -> new S2CMessageStunGrenade(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
    );

    public static void handle(S2CMessageStunGrenade message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleExplosionStunGrenade(message));
        context.setHandled(true);
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getZ()
    {
        return this.z;
    }
}
