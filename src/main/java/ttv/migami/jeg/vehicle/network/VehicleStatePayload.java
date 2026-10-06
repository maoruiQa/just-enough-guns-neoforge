package ttv.migami.jeg.vehicle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record VehicleStatePayload(int vehicleId, double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float pitch, boolean forceApply, double power, double steering, double rudder, float roll, boolean engineStarted, boolean engineReady) implements CustomPacketPayload {
    public VehicleStatePayload(int vehicleId, double x, double y, double z, double motionX, double motionY, double motionZ, float yaw, float pitch, boolean forceApply) {
        this(vehicleId,x,y,z,motionX,motionY,motionZ,yaw,pitch,forceApply,Double.NaN,Double.NaN,Double.NaN,0,false,false);
    }

    public static final Type<VehicleStatePayload> TYPE = new Type<>(Reference.id("vehicle_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.vehicleId());
                buf.writeDouble(payload.x());
                buf.writeDouble(payload.y());
                buf.writeDouble(payload.z());
                buf.writeDouble(payload.motionX());
                buf.writeDouble(payload.motionY());
                buf.writeDouble(payload.motionZ());
                buf.writeFloat(payload.yaw());
                buf.writeFloat(payload.pitch());
                buf.writeBoolean(payload.forceApply());
                buf.writeDouble(payload.power());
                buf.writeDouble(payload.steering());
                buf.writeDouble(payload.rudder());
                buf.writeFloat(payload.roll());
                buf.writeBoolean(payload.engineStarted());
                buf.writeBoolean(payload.engineReady());
            },
            buf -> new VehicleStatePayload(
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readBoolean()
            )
    );

    @Override
    public Type<VehicleStatePayload> type() {
        return TYPE;
    }
}
