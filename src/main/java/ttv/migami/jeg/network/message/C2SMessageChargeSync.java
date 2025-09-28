package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record C2SMessageChargeSync(float chargeProgress)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageChargeSync> CODEC = StreamCodec.of(
            (buffer, message) -> buffer.writeFloat(message.chargeProgress),
            buffer -> new C2SMessageChargeSync(buffer.readFloat())
    );

    public static void handle(C2SMessageChargeSync message, PlayMessageContext context)
    {
        context.execute(() -> context.getPlayer().ifPresent(player -> player.getPersistentData().putFloat("ChargeProgress", message.chargeProgress())));
        context.setHandled(true);
    }
}
