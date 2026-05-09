package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record VehicleChangeSeatPayload(int vehicleId) implements CustomPacketPayload {
    public static final Type<VehicleChangeSeatPayload> TYPE = new Type<>(Reference.id("vehicle_change_seat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleChangeSeatPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.vehicleId()),
            buf -> new VehicleChangeSeatPayload(buf.readVarInt())
    );

    @Override
    public Type<VehicleChangeSeatPayload> type() {
        return TYPE;
    }
}
