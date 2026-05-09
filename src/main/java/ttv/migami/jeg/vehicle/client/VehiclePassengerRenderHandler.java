package ttv.migami.jeg.vehicle.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehiclePassengerRenderHandler {
    private VehiclePassengerRenderHandler() {}

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof VehicleEntity vehicle && vehicle.shouldHidePassenger(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
