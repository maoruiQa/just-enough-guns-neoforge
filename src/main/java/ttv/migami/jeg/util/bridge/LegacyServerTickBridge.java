package ttv.migami.jeg.util.bridge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ttv.migami.jeg.Reference;

/**
 * Re-emits server tick callbacks in the legacy Forge shape used by the mod.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class LegacyServerTickBridge {

    private LegacyServerTickBridge() {}

    @SubscribeEvent
    public static void handleServerTickPre(ServerTickEvent.Pre event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(event::hasTime, event.getServer(), TickEvent.Phase.START));
    }

    @SubscribeEvent
    public static void handleServerTickPost(ServerTickEvent.Post event) {
        NeoForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(event::hasTime, event.getServer(), TickEvent.Phase.END));
    }
}
