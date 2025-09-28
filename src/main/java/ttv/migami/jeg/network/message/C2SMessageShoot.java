package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageShoot(float rotationYaw, float rotationPitch)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageShoot> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeFloat(message.rotationYaw);
                buffer.writeFloat(message.rotationPitch);
            },
            buffer -> new C2SMessageShoot(buffer.readFloat(), buffer.readFloat())
    );

    public C2SMessageShoot(Player player)
    {
        this(player.getYRot(), player.getXRot());
    }

    public static void handle(C2SMessageShoot message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null)
            {
                ServerPlayHandler.handleShoot(message, player);
            }
        });
        context.setHandled(true);
    }

    public float getRotationYaw()
    {
        return this.rotationYaw;
    }

    public float getRotationPitch()
    {
        return this.rotationPitch;
    }
}
