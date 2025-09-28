package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.client.network.ClientPlayHandler;

/**
 * Author: MrCrayfish
 */
public record S2CMessageProjectileHitBlock(double x, double y, double z, BlockPos pos, Direction face)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageProjectileHitBlock> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeDouble(message.x);
                buffer.writeDouble(message.y);
                buffer.writeDouble(message.z);
                buffer.writeBlockPos(message.pos);
                buffer.writeEnum(message.face);
            },
            buffer -> new S2CMessageProjectileHitBlock(
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBlockPos(),
                    buffer.readEnum(Direction.class)
            )
    );

    public static void handle(S2CMessageProjectileHitBlock message, PlayMessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleProjectileHitBlock(message));
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

    public BlockPos getPos()
    {
        return this.pos;
    }

    public Direction getFace()
    {
        return this.face;
    }
}
