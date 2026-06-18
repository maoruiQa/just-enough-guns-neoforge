package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record InspectGunPayload() implements CustomPacketPayload {
    public static final InspectGunPayload INSTANCE = new InspectGunPayload();
    public static final Type<InspectGunPayload> TYPE = new Type<>(Reference.id("inspect_gun"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InspectGunPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<InspectGunPayload> type() {
        return TYPE;
    }
}
