package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record ShootRequestPayload(InteractionHand hand) implements CustomPacketPayload {
    public static final Type<ShootRequestPayload> TYPE = new Type<>(Reference.id("shoot_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShootRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeEnum(payload.hand()),
            buf -> new ShootRequestPayload(buf.readEnum(InteractionHand.class))
    );

    @Override
    public Type<ShootRequestPayload> type() {
        return TYPE;
    }
}
