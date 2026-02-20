package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;

/**
 * Network payload with per-bullet trail state for 1.20.x-style client rendering.
 */
public record BulletTrailPayload(
        int[] entityIds,
        Vec3[] positions,
        Vec3[] motions,
        int color,
        float size,
        int life,
        double gravity,
        int shooterId,
        boolean trailVisible
) implements CustomPacketPayload {
    public static final Type<BulletTrailPayload> TYPE = new Type<>(Reference.id("bullet_trail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulletTrailPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                int count = Math.min(
                        payload.entityIds.length,
                        Math.min(payload.positions.length, payload.motions.length)
                );
                buf.writeInt(count);
                for (int i = 0; i < count; i++) {
                    buf.writeInt(payload.entityIds[i]);
                    buf.writeDouble(payload.positions[i].x);
                    buf.writeDouble(payload.positions[i].y);
                    buf.writeDouble(payload.positions[i].z);
                    buf.writeDouble(payload.motions[i].x);
                    buf.writeDouble(payload.motions[i].y);
                    buf.writeDouble(payload.motions[i].z);
                }
                buf.writeInt(payload.color);
                buf.writeFloat(payload.size);
                buf.writeInt(payload.life);
                buf.writeDouble(payload.gravity);
                buf.writeInt(payload.shooterId);
                buf.writeBoolean(payload.trailVisible);
            },
            buf -> {
                int count = buf.readInt();
                int[] entityIds = new int[count];
                Vec3[] positions = new Vec3[count];
                Vec3[] motions = new Vec3[count];
                for (int i = 0; i < count; i++) {
                    entityIds[i] = buf.readInt();
                    positions[i] = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    motions[i] = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                }
                return new BulletTrailPayload(
                        entityIds,
                        positions,
                        motions,
                        buf.readInt(),
                        buf.readFloat(),
                        buf.readInt(),
                        buf.readDouble(),
                        buf.readInt(),
                        buf.readBoolean()
                );
            }
    );

    public BulletTrailPayload(
            int entityId,
            Vec3 position,
            Vec3 motion,
            int color,
            float size,
            int life,
            double gravity,
            int shooterId,
            boolean trailVisible
    ) {
        this(
                new int[]{entityId},
                new Vec3[]{position},
                new Vec3[]{motion},
                color,
                size,
                life,
                gravity,
                shooterId,
                trailVisible
        );
    }

    @Override
    public Type<BulletTrailPayload> type() {
        return TYPE;
    }
}
