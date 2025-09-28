package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.common.network.ServerPlayHandler;

public record C2SMessageToggleMedals()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageToggleMedals> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageToggleMedals()
    );

    public static void handle(C2SMessageToggleMedals message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null && !player.isSpectator())
            {
                ServerPlayHandler.toggleMedals(player);
            }
        });
        context.setHandled(true);
    }
}
