package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record MedalPayload(MedalType medal) implements CustomPacketPayload {
    public static final Type<MedalPayload> TYPE = new Type<>(Reference.id("medal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MedalPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.medal.ordinal()),
            buf -> new MedalPayload(MedalType.byOrdinal(buf.readVarInt()))
    );

    @Override
    public Type<MedalPayload> type() {
        return TYPE;
    }
}
