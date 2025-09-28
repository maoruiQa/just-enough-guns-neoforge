package ttv.migami.jeg.network.message;

import com.mrcrayfish.framework.api.network.PlayMessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.medal.MedalManager;

/**
 * Author: MrCrayfish
 */
public record S2CMessageSendKillMedal()
{
    private static final ResourceLocation KILL_SINGLE = new ResourceLocation(Reference.MOD_ID, "textures/gui/medal/multikill_kill_single.png");

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CMessageSendKillMedal> CODEC = StreamCodec.of(
            (buffer, message) -> {},
            buffer -> new S2CMessageSendKillMedal()
    );

    public static void handle(S2CMessageSendKillMedal message, PlayMessageContext context)
    {
        context.execute(() -> MedalManager.addKillMedal(KILL_SINGLE, Component.translatable("medal.jeg.multikill_kill_single")));
        context.setHandled(true);
    }
}
