package ttv.migami.jeg.event;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.network.NetworkHandler;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class ExplosionShakeEvents {
    private ExplosionShakeEvents() {}

    @SubscribeEvent
    public static void detonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            NetworkHandler.sendExplosionShake(level, event.getExplosion().center(), event.getExplosion().radius());
        }
    }
}
