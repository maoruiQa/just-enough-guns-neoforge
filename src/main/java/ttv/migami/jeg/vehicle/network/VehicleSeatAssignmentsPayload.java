package ttv.migami.jeg.vehicle.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record VehicleSeatAssignmentsPayload(int vehicleId, UUID[] passengerIds, int[] seatIndices) implements CustomPacketPayload {
    public static final Type<VehicleSeatAssignmentsPayload> TYPE = new Type<>(Reference.id("vehicle_seat_assignments"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleSeatAssignmentsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.vehicleId());
                int count = Math.min(payload.passengerIds().length, payload.seatIndices().length);
                buf.writeVarInt(count);
                for (int index = 0; index < count; index++) {
                    buf.writeUUID(payload.passengerIds()[index]);
                    buf.writeVarInt(payload.seatIndices()[index]);
                }
            },
            buf -> {
                int vehicleId = buf.readVarInt();
                int count = buf.readVarInt();
                UUID[] passengerIds = new UUID[count];
                int[] seatIndices = new int[count];
                for (int index = 0; index < count; index++) {
                    passengerIds[index] = buf.readUUID();
                    seatIndices[index] = buf.readVarInt();
                }
                return new VehicleSeatAssignmentsPayload(vehicleId, passengerIds, seatIndices);
            }
    );

    public static VehicleSeatAssignmentsPayload fromMap(int vehicleId, Map<UUID, Integer> assignments) {
        UUID[] passengerIds = new UUID[assignments.size()];
        int[] seatIndices = new int[assignments.size()];
        int index = 0;
        for (Map.Entry<UUID, Integer> assignment : assignments.entrySet()) {
            passengerIds[index] = assignment.getKey();
            seatIndices[index] = assignment.getValue();
            index++;
        }
        return new VehicleSeatAssignmentsPayload(vehicleId, passengerIds, seatIndices);
    }

    public Map<UUID, Integer> toMap() {
        Map<UUID, Integer> assignments = new HashMap<>();
        int count = Math.min(this.passengerIds.length, this.seatIndices.length);
        for (int index = 0; index < count; index++) {
            assignments.put(this.passengerIds[index], this.seatIndices[index]);
        }
        return assignments;
    }

    @Override
    public Type<VehicleSeatAssignmentsPayload> type() {
        return TYPE;
    }
}
