package ttv.migami.jeg.vehicle.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;

public record VehicleAssemblyRecipeSyncPayload(Map<ResourceLocation, String> recipes) implements CustomPacketPayload {
    public static final Type<VehicleAssemblyRecipeSyncPayload> TYPE = new Type<>(Reference.id("vehicle_assembly_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblyRecipeSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.recipes().size());
                for (Map.Entry<ResourceLocation, String> entry : payload.recipes().entrySet()) {
                    buf.writeUtf(entry.getKey().toString());
                    buf.writeUtf(entry.getValue(), 1_048_576);
                }
            },
            buf -> {
                int count = buf.readVarInt();
                Map<ResourceLocation, String> recipes = new HashMap<>();
                for (int index = 0; index < count; index++) {
                    recipes.put(ResourceLocation.parse(buf.readUtf()), buf.readUtf(1_048_576));
                }
                return new VehicleAssemblyRecipeSyncPayload(recipes);
            }
    );

    public VehicleAssemblyRecipeSyncPayload {
        recipes = Map.copyOf(recipes);
    }

    @Override
    public Type<VehicleAssemblyRecipeSyncPayload> type() {
        return TYPE;
    }
}
