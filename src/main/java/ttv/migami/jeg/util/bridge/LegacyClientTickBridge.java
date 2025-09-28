package ttv.migami.jeg.util.bridge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import ttv.migami.jeg.Reference;

/**
 * Bridges modern NeoForge client tick/frame events back to the legacy {@link TickEvent}
 * variants used throughout the mod. This lets the existing handlers continue to work while
 * the port progresses.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class LegacyClientTickBridge {

    private LegacyClientTickBridge() {}

    @SubscribeEvent
    public static void handleClientTickPre(ClientTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START, Minecraft.getInstance()));
    }

    @SubscribeEvent
    public static void handleClientTickPost(ClientTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END, Minecraft.getInstance()));
    }

    @SubscribeEvent
    public static void handleRenderFramePre(RenderFrameEvent.Pre event) {
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        NeoForge.EVENT_BUS.post(new TickEvent.RenderTickEvent(TickEvent.Phase.START, partialTicks));
    }

    @SubscribeEvent
    public static void handleRenderFramePost(RenderFrameEvent.Post event) {
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        NeoForge.EVENT_BUS.post(new TickEvent.RenderTickEvent(TickEvent.Phase.END, partialTicks));
    }
}
