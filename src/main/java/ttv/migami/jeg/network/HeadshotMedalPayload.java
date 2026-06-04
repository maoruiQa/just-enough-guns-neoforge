package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record HeadshotMedalPayload() implements CustomPacketPayload {
    public static final HeadshotMedalPayload INSTANCE = new HeadshotMedalPayload();
    public static final Type<HeadshotMedalPayload> TYPE = new Type<>(Reference.id("headshot_medal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeadshotMedalPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<HeadshotMedalPayload> type() {
        return TYPE;
    }
}
