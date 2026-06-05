package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ChargeFlashlightPayload() implements CustomPacketPayload {
    public static final ChargeFlashlightPayload INSTANCE = new ChargeFlashlightPayload();
    public static final Type<ChargeFlashlightPayload> TYPE = new Type<>(Reference.id("charge_flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargeFlashlightPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<ChargeFlashlightPayload> type() {
        return TYPE;
    }
}
