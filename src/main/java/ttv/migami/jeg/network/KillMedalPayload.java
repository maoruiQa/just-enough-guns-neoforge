package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record KillMedalPayload() implements CustomPacketPayload {
    public static final KillMedalPayload INSTANCE = new KillMedalPayload();
    public static final Type<KillMedalPayload> TYPE = new Type<>(Reference.id("kill_medal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KillMedalPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<KillMedalPayload> type() {
        return TYPE;
    }
}
