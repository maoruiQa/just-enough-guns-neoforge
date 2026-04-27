package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record HoldFirePayload(InteractionHand hand, boolean holding) implements CustomPacketPayload {
    public static final Type<HoldFirePayload> TYPE = new Type<>(Reference.id("hold_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HoldFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.hand());
                buf.writeBoolean(payload.holding());
            },
            buf -> new HoldFirePayload(buf.readEnum(InteractionHand.class), buf.readBoolean())
    );

    @Override
    public Type<HoldFirePayload> type() {
        return TYPE;
    }
}
