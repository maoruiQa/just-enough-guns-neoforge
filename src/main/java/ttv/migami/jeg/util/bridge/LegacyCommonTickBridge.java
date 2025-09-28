package ttv.migami.jeg.util.bridge;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ttv.migami.jeg.Reference;

/**
 * Forwards the modern {@link PlayerTickEvent} callbacks into the legacy TickEvent form.
 */
@EventBusSubscriber(modid = Reference.MOD_ID)
public final class LegacyCommonTickBridge {

    private LegacyCommonTickBridge() {}

    @SubscribeEvent
    public static void handlePlayerTickPre(PlayerTickEvent.Pre event) {
        forward(event, TickEvent.Phase.START);
    }

    @SubscribeEvent
    public static void handlePlayerTickPost(PlayerTickEvent.Post event) {
        forward(event, TickEvent.Phase.END);
    }

    private static void forward(PlayerTickEvent event, TickEvent.Phase phase) {
        Player player = event.getEntity();
        LogicalSide side = player.level().isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER;
        NeoForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(player, side, phase));
    }
}
