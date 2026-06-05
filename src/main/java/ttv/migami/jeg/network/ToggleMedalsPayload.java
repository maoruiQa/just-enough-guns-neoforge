package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ToggleMedalsPayload() implements CustomPacketPayload {
    public static final ToggleMedalsPayload INSTANCE = new ToggleMedalsPayload();
    public static final Type<ToggleMedalsPayload> TYPE = new Type<>(Reference.id("toggle_medals"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMedalsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<ToggleMedalsPayload> type() {
        return TYPE;
    }
}
