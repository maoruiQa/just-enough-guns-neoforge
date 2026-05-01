package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import ttv.migami.jeg.Reference;

public record ShootRequestPayload(InteractionHand hand, boolean aiming) implements CustomPacketPayload {
    public static final Type<ShootRequestPayload> TYPE = new Type<>(Reference.id("shoot_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShootRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.hand());
                buf.writeBoolean(payload.aiming());
            },
            buf -> new ShootRequestPayload(buf.readEnum(InteractionHand.class), buf.readBoolean())
    );

    @Override
    public Type<ShootRequestPayload> type() {
        return TYPE;
    }
}
