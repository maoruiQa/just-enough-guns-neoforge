package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Network payload to send bullet trail data from server to client.
 * The server calculates the complete bullet path (including penetration)
 * and sends it to clients for rendering.
 */
public record BulletTrailPayload(Vec3 start, Vec3 end, int color, float size, boolean hitSolid) implements CustomPacketPayload {
    public static final Type<BulletTrailPayload> TYPE = new Type<>(Reference.id("bullet_trail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulletTrailPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeDouble(payload.start.x);
                buf.writeDouble(payload.start.y);
                buf.writeDouble(payload.start.z);
                buf.writeDouble(payload.end.x);
                buf.writeDouble(payload.end.y);
                buf.writeDouble(payload.end.z);
                buf.writeInt(payload.color);
                buf.writeFloat(payload.size);
                buf.writeBoolean(payload.hitSolid);
            },
            buf -> new BulletTrailPayload(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readInt(),
                buf.readFloat(),
                buf.readBoolean()
            )
    );

    @Override
    public Type<BulletTrailPayload> type() {
        return TYPE;
    }
}
