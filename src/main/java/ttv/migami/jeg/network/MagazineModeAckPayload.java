package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record MagazineModeAckPayload(long revision) implements CustomPacketPayload {
    public static final Type<MagazineModeAckPayload> TYPE = new Type<>(Reference.id("magazine_mode_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagazineModeAckPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeLong(payload.revision()), buf -> new MagazineModeAckPayload(buf.readLong()));

    @Override public Type<MagazineModeAckPayload> type() { return TYPE; }
}
