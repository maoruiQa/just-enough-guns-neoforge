package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record AssembleTestVehiclePayload() implements CustomPacketPayload {
    public static final AssembleTestVehiclePayload INSTANCE = new AssembleTestVehiclePayload();
    public static final Type<AssembleTestVehiclePayload> TYPE = new Type<>(Reference.id("assemble_test_vehicle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssembleTestVehiclePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<AssembleTestVehiclePayload> type() {
        return TYPE;
    }
}
