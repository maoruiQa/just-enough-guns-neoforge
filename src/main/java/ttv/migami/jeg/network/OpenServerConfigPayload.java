package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record OpenServerConfigPayload() implements CustomPacketPayload {
    public static final OpenServerConfigPayload INSTANCE = new OpenServerConfigPayload();
    public static final Type<OpenServerConfigPayload> TYPE = new Type<>(Reference.id("open_server_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenServerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Type<OpenServerConfigPayload> type() {
        return TYPE;
    }
}
