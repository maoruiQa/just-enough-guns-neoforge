package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record OffhandFullPromptPayload() implements CustomPacketPayload {
    public static final OffhandFullPromptPayload INSTANCE = new OffhandFullPromptPayload();
    public static final Type<OffhandFullPromptPayload> TYPE = new Type<>(Reference.id("offhand_full_prompt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OffhandFullPromptPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<OffhandFullPromptPayload> type() {
        return TYPE;
    }
}
