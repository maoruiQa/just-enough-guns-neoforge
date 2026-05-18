package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record VehicleDismountPayload(int vehicleId) implements CustomPacketPayload {
    public static final Type<VehicleDismountPayload> TYPE = new Type<>(Reference.id("vehicle_dismount"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleDismountPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.vehicleId()),
            buf -> new VehicleDismountPayload(buf.readVarInt())
    );

    @Override
    public Type<VehicleDismountPayload> type() {
        return TYPE;
    }
}
