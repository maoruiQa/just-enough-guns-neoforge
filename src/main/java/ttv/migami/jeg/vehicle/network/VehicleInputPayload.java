package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;

public record VehicleInputPayload(
        int vehicleId,
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean brake,
        boolean ascend,
        boolean descend,
        boolean fire,
        boolean reload,
        boolean freeLook,
        boolean switchWeapon,
        boolean previousWeapon,
        int weaponSlot,
        boolean seekTarget,
        boolean deployDecoy,
        float mouseX,
        float mouseY
) implements CustomPacketPayload {
    public static final Type<VehicleInputPayload> TYPE = new Type<>(Reference.id("vehicle_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleInputPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.vehicleId());
                buf.writeBoolean(payload.forward());
                buf.writeBoolean(payload.backward());
                buf.writeBoolean(payload.left());
                buf.writeBoolean(payload.right());
                buf.writeBoolean(payload.brake());
                buf.writeBoolean(payload.ascend());
                buf.writeBoolean(payload.descend());
                buf.writeBoolean(payload.fire());
                buf.writeBoolean(payload.reload());
                buf.writeBoolean(payload.freeLook());
                buf.writeBoolean(payload.switchWeapon());
                buf.writeBoolean(payload.previousWeapon());
                buf.writeVarInt(payload.weaponSlot());
                buf.writeBoolean(payload.seekTarget());
                buf.writeBoolean(payload.deployDecoy());
                buf.writeFloat(payload.mouseX());
                buf.writeFloat(payload.mouseY());
            },
            buf -> new VehicleInputPayload(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    public VehicleInput toInput() {
        return new VehicleInput(this.forward, this.backward, this.left, this.right, this.brake, this.ascend, this.descend, this.fire, this.reload, this.freeLook, this.switchWeapon, this.previousWeapon, this.weaponSlot, this.seekTarget, this.deployDecoy, this.mouseX, this.mouseY);
    }

    @Override
    public Type<VehicleInputPayload> type() {
        return TYPE;
    }
}
