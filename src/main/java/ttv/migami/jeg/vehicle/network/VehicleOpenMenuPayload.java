package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record VehicleOpenMenuPayload(int vehicleId) implements CustomPacketPayload {
    public static final Type<VehicleOpenMenuPayload> TYPE = new Type<>(Reference.id("vehicle_open_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleOpenMenuPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.vehicleId()),
            buf -> new VehicleOpenMenuPayload(buf.readVarInt())
    );

    @Override
    public Type<VehicleOpenMenuPayload> type() {
        return TYPE;
    }
}
