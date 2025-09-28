package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 */
public record S2CMessageProjectileHitEntity(double x, double y, double z, int type, boolean player)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageProjectileHitEntity> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeDouble(message.x);
                buffer.writeDouble(message.y);
                buffer.writeDouble(message.z);
                buffer.writeByte(message.type);
                buffer.writeBoolean(message.player);
            },
            buffer -> new S2CMessageProjectileHitEntity(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readByte(),
                    buffer.readBoolean()
            )
    );

    public static void handle(S2CMessageProjectileHitEntity message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleProjectileHitEntity(message));
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

    public boolean isHeadshot()
    {
        return this.type == HitType.HEADSHOT;
    }

    public boolean isCritical()
    {
        return this.type == HitType.CRITICAL;
    }

    public boolean isPlayer()
    {
        return this.player;
    }

    public static class HitType
    {
        public static final int NORMAL = 0;
        public static final int HEADSHOT = 1;
        public static final int CRITICAL = 2;
    }
}
