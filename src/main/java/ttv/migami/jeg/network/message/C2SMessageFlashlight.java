package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.event.FlashlightEvent;

/**
 * Author: MrCrayfish
 */
public record C2SMessageFlashlight()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageFlashlight> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageFlashlight()
    );

    public static void handle(C2SMessageFlashlight message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null && !player.isSpectator())
            {
                FlashlightEvent.chargeFlashlight(player);
            }
        });
        context.setHandled(true);
    }
}
