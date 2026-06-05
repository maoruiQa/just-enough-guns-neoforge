package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record UiConfigPayload(boolean showCrosshair, boolean showHitFeedback, boolean hideMedals) implements CustomPacketPayload {
    public static final Type<UiConfigPayload> TYPE = new Type<>(Reference.id("ui_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UiConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.showCrosshair);
                buf.writeBoolean(payload.showHitFeedback);
                buf.writeBoolean(payload.hideMedals);
            },
            buf -> new UiConfigPayload(buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<UiConfigPayload> type() {
        return TYPE;
    }
}
