package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record HitMarkerPayload(boolean critical) implements CustomPacketPayload {
    public static final Type<HitMarkerPayload> TYPE = new Type<>(Reference.id("hit_marker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HitMarkerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.critical),
            buf -> new HitMarkerPayload(buf.readBoolean())
    );

    @Override
    public Type<HitMarkerPayload> type() {
        return TYPE;
    }
}
