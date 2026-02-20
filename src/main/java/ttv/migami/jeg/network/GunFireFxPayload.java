package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record GunFireFxPayload(int shooterId, float randomValue) implements CustomPacketPayload {
    public static final Type<GunFireFxPayload> TYPE = new Type<>(Reference.id("gun_fire_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunFireFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.shooterId);
                buf.writeFloat(payload.randomValue);
            },
            buf -> new GunFireFxPayload(buf.readInt(), buf.readFloat())
    );

    @Override
    public Type<GunFireFxPayload> type() {
        return TYPE;
    }
}
