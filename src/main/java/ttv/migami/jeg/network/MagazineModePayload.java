package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record MagazineModePayload(boolean magazineFeed, long revision, boolean changed) implements CustomPacketPayload {
    public static final Type<MagazineModePayload> TYPE = new Type<>(Reference.id("magazine_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MagazineModePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.magazineFeed());
                buf.writeLong(payload.revision());
                buf.writeBoolean(payload.changed());
            }, buf -> new MagazineModePayload(buf.readBoolean(), buf.readLong(), buf.readBoolean()));

    @Override public Type<MagazineModePayload> type() { return TYPE; }
}
