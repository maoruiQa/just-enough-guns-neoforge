package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record OpenAttachmentsPayload() implements CustomPacketPayload {
    public static final OpenAttachmentsPayload INSTANCE = new OpenAttachmentsPayload();
    public static final Type<OpenAttachmentsPayload> TYPE = new Type<>(Reference.id("open_attachments"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAttachmentsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
            },
            buf -> INSTANCE
    );

    @Override
    public Type<OpenAttachmentsPayload> type() {
        return TYPE;
    }
}
