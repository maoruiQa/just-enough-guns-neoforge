package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record MeleePayload() implements CustomPacketPayload {
    public static final MeleePayload INSTANCE = new MeleePayload();
    public static final Type<MeleePayload> TYPE = new Type<>(Reference.id("melee"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MeleePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<MeleePayload> type() {
        return TYPE;
    }
}
