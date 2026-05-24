package ttv.migami.jeg.vehicle.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.Reference;

public record VehicleDataSyncPayload(Map<Identifier, String> data) implements CustomPacketPayload {
    public static final Type<VehicleDataSyncPayload> TYPE = new Type<>(Reference.id("vehicle_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleDataSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.data().size());
                for (Map.Entry<Identifier, String> entry : payload.data().entrySet()) {
                    buf.writeUtf(entry.getKey().toString());
                    buf.writeUtf(entry.getValue(), 1_048_576);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                Map<Identifier, String> data = new HashMap<>();
                for (int index = 0; index < count; index++) {
                    data.put(Identifier.parse(buf.readUtf()), buf.readUtf(1_048_576));
                }
                return new VehicleDataSyncPayload(data);
            }
    );

    public VehicleDataSyncPayload {
        data = Map.copyOf(data);
    }

    @Override
    public Type<VehicleDataSyncPayload> type() {
        return TYPE;
    }
}
