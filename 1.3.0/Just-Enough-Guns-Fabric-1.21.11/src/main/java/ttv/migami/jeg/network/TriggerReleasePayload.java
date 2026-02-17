package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record TriggerReleasePayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<TriggerReleasePayload> TYPE = new Type<>(Reference.id("trigger_release"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerReleasePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeEnum(payload.hand),
            buf -> new TriggerReleasePayload(buf.readEnum(InteractionHand.class))
    );

    @Override
    public Type<TriggerReleasePayload> type() {
        return TYPE;
    }
}
