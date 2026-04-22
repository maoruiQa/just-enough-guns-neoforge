package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record UnloadMagazineRequestPayload() implements CustomPacketPayload {
    public static final UnloadMagazineRequestPayload INSTANCE = new UnloadMagazineRequestPayload();
    public static final Type<UnloadMagazineRequestPayload> TYPE = new Type<>(Reference.id("unload_magazine_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UnloadMagazineRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<UnloadMagazineRequestPayload> type() {
        return TYPE;
    }
}
