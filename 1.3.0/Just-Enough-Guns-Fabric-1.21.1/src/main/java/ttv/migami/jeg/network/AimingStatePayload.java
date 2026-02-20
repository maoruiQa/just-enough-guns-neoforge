package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record AimingStatePayload(boolean aiming) implements CustomPacketPayload {
    public static final Type<AimingStatePayload> TYPE = new Type<>(Reference.id("aiming_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AimingStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.aiming()),
            buf -> new AimingStatePayload(buf.readBoolean())
    );

    @Override
    public Type<AimingStatePayload> type() {
        return TYPE;
    }
}
