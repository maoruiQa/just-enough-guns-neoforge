package ttv.migami.jeg.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonGunArmRenderEvents {
    private FirstPersonGunArmRenderEvents() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // Align with NeoForge 1.21.11: no event-based first-person arm overlay.
        // Gun arms are rendered by GeckoLib arm bones in GunFirstPersonArmsLayer.
        return;
    }
}
