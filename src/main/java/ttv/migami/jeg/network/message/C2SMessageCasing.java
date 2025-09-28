package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.event.GunEventBus;

/**
 * Author: MrCrayfish
 */
public record C2SMessageCasing()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageCasing> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new C2SMessageCasing()
    );

    public static void handle(C2SMessageCasing message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(player -> {
            if(!player.isSpectator())
            {
                GunEventBus.ejectCasing(player.level(), player);
            }
        }));
        context.setHandled(true);
    }
}
