package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record ReloadRequestPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ReloadRequestPayload> TYPE = new Type<>(Reference.id("reload_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReloadRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeEnum(payload.hand),
            buf -> new ReloadRequestPayload(buf.readEnum(InteractionHand.class))
    );

    @Override
    public Type<ReloadRequestPayload> type() {
        return TYPE;
    }
}

