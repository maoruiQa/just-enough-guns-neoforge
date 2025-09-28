package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.init.ModSyncedDataKeys;

public record C2SMessageAim(boolean aiming)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageAim> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeBoolean(message.aiming),
            buffer -> new C2SMessageAim(buffer.readBoolean())
    );

    public static void handle(C2SMessageAim message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(player -> {
            if(!player.isSpectator())
            {
                ModSyncedDataKeys.AIMING.setValue(player, message.aiming);
            }
        }));
        context.setHandled(true);
    }
}
