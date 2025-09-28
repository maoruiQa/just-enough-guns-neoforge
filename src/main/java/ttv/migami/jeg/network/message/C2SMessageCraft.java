package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ttv.migami.jeg.common.network.ServerPlayHandler;

/**
 * Author: MrCrayfish
 */
public record C2SMessageCraft(ResourceLocation id, BlockPos pos)
{
    public static final StreamCodec<RegistryFriendlyByteBuf, C2SMessageCraft> CODEC = StreamCodec.of(
            (buffer, message) -> {
                buffer.writeResourceLocation(message.id);
                buffer.writeBlockPos(message.pos);
            },
            buffer -> new C2SMessageCraft(buffer.readResourceLocation(), buffer.readBlockPos())
    );

    public static void handle(C2SMessageCraft message, PlayMessageContext context)
    {
        context.execute(() -> {
            ServerPlayer player = context.getPlayer().orElse(null);
            if(player != null)
            {
                ServerPlayHandler.handleCraft(player, message.id, message.pos);
            }
        });
        context.setHandled(true);
    }
}
