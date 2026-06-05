package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ToggleFlashlightPayload() implements CustomPacketPayload {
    public static final ToggleFlashlightPayload INSTANCE = new ToggleFlashlightPayload();
    public static final Type<ToggleFlashlightPayload> TYPE = new Type<>(Reference.id("toggle_flashlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleFlashlightPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<ToggleFlashlightPayload> type() {
        return TYPE;
    }
}
