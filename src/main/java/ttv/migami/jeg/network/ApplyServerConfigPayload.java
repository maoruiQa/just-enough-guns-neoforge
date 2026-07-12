package ttv.migami.jeg.network;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ApplyServerConfigPayload(Map<String, String> changes) implements CustomPacketPayload {
    public static final Type<ApplyServerConfigPayload> TYPE = new Type<>(Reference.id("apply_server_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ApplyServerConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> writeMap(buf, payload.changes),
            buf -> new ApplyServerConfigPayload(readMap(buf))
    );

    public ApplyServerConfigPayload {
        changes = Map.copyOf(changes);
    }

    @Override
    public Type<ApplyServerConfigPayload> type() {
        return TYPE;
    }

    static void writeMap(RegistryFriendlyByteBuf buf, Map<String, String> values) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            buf.writeUtf(entry.getKey(), 256);
            buf.writeUtf(entry.getValue(), 1024);
        }
    }

    static Map<String, String> readMap(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 256) {
            throw new IllegalArgumentException("Invalid config entry count: " + size);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 0; index < size; index++) {
            values.put(buf.readUtf(256), buf.readUtf(1024));
        }
        return values;
    }
}
