package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record GuidedLockPayload(InteractionHand hand, int targetId) implements CustomPacketPayload {
    public static final Type<GuidedLockPayload> TYPE = new Type<>(Reference.id("guided_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuidedLockPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.hand());
                buf.writeVarInt(payload.targetId() + 1);
            },
            buf -> new GuidedLockPayload(buf.readEnum(InteractionHand.class), buf.readVarInt() - 1)
    );

    @Override
    public Type<GuidedLockPayload> type() {
        return TYPE;
    }
}
