package ttv.migami.jeg.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ServerConfigStatePayload(Status status, Map<String, String> values, List<String> invalidKeys, int changedCount) implements CustomPacketPayload {
    public enum Status {
        OPEN,
        APPLIED,
        DENIED,
        INVALID
    }

    public static final Type<ServerConfigStatePayload> TYPE = new Type<>(Reference.id("server_config_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeByte(payload.status.ordinal());
                ApplyServerConfigPayload.writeMap(buf, payload.values);
                buf.writeVarInt(payload.invalidKeys.size());
                for (String key : payload.invalidKeys) {
                    buf.writeUtf(key, 256);
                }
                buf.writeVarInt(payload.changedCount);
            },
            buf -> {
                int statusId = buf.readUnsignedByte();
                Status[] statuses = Status.values();
                if (statusId >= statuses.length) {
                    throw new IllegalArgumentException("Unknown server config state: " + statusId);
                }
                Map<String, String> values = ApplyServerConfigPayload.readMap(buf);
                int invalidCount = buf.readVarInt();
                if (invalidCount < 0 || invalidCount > ApplyServerConfigPayload.MAX_CONFIG_ENTRIES) {
                    throw new IllegalArgumentException("Invalid config error count: " + invalidCount);
                }
                List<String> invalidKeys = new ArrayList<>();
                for (int index = 0; index < invalidCount; index++) {
                    invalidKeys.add(buf.readUtf(256));
                }
                return new ServerConfigStatePayload(statuses[statusId], values, invalidKeys, buf.readVarInt());
            }
    );

    public ServerConfigStatePayload {
        values = Map.copyOf(values);
        invalidKeys = List.copyOf(invalidKeys);
    }

    @Override
    public Type<ServerConfigStatePayload> type() {
        return TYPE;
    }
}
