package ttv.migami.jeg.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import ttv.migami.jeg.Reference;

public record ToggleLauncherModePayload() implements CustomPacketPayload {
    public static final ToggleLauncherModePayload INSTANCE = new ToggleLauncherModePayload();
    public static final Type<ToggleLauncherModePayload> TYPE = new Type<>(Reference.id("toggle_launcher_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleLauncherModePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ToggleLauncherModePayload> type() {
        return TYPE;
    }
}
