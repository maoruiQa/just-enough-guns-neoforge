package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record DroneInputPayload(int entityId, int inputs, float yawDelta, float pitchDelta) implements CustomPacketPayload {
    public static final Type<DroneInputPayload> TYPE = new Type<>(Reference.id("drone_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeVarInt(payload.inputs());
                buf.writeFloat(payload.yawDelta());
                buf.writeFloat(payload.pitchDelta());
            },
            buf -> new DroneInputPayload(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat())
    );

    @Override
    public Type<DroneInputPayload> type() {
        return TYPE;
    }
}
