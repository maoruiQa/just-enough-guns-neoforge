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
public record C2SMessagePreFireSound()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessagePreFireSound> CODEC = StreamCodec.of(
            (buffer, message) -> {
            },
            buffer -> new C2SMessagePreFireSound()
    );

    public C2SMessagePreFireSound(Player player)
    {
        this();
    }

    public static void handle(C2SMessagePreFireSound message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null)
            {
                ServerPlayHandler.handlePreFireSound(message, player);
            }
        });
        context.setHandled(true);
    }
}
