package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ExplosionShakePayload(double x, double y, double z, double radius, double time, double amplitude)
        implements CustomPacketPayload {
    public static final Type<ExplosionShakePayload> TYPE = new Type<>(Reference.id("explosion_shake"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionShakePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z);
                buf.writeDouble(p.radius); buf.writeDouble(p.time); buf.writeDouble(p.amplitude);
            },
            buf -> new ExplosionShakePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    public boolean valid() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && Double.isFinite(radius) && radius > 0 && Double.isFinite(time) && time > 0
                && Double.isFinite(amplitude) && amplitude > 0;
    }

    @Override
    public Type<ExplosionShakePayload> type() { return TYPE; }
}
