package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record DroneControlPayload(int entityId, boolean active, int maxRange) implements CustomPacketPayload {
    public static final Type<DroneControlPayload> TYPE = new Type<>(Reference.id("drone_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.entityId());
                buf.writeBoolean(payload.active());
                buf.writeVarInt(payload.maxRange());
            },
            buf -> new DroneControlPayload(buf.readVarInt(), buf.readBoolean(), buf.readVarInt())
    );

    @Override
    public Type<DroneControlPayload> type() {
        return TYPE;
    }
}
