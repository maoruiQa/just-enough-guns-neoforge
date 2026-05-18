package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.Reference;

public record AssembleTestVehiclePayload(Identifier recipeId) implements CustomPacketPayload {
    public static final Type<AssembleTestVehiclePayload> TYPE = new Type<>(Reference.id("assemble_test_vehicle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AssembleTestVehiclePayload> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(AssembleTestVehiclePayload::new, AssembleTestVehiclePayload::recipeId).cast();

    @Override
    public Type<AssembleTestVehiclePayload> type() {
        return TYPE;
    }
}
