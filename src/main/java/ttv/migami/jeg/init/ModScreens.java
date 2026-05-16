package ttv.migami.jeg.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.client.screen.VehicleAssemblingScreen;
import ttv.migami.jeg.vehicle.client.screen.VehicleChargingStationScreen;
import ttv.migami.jeg.vehicle.client.screen.VehicleScreen;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ModScreens {
    private ModScreens() {}

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.VEHICLE_MENU.get(), VehicleScreen::new);
        event.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), VehicleAssemblingScreen::new);
        event.register(ModMenuTypes.VEHICLE_CHARGING_STATION_MENU.get(), VehicleChargingStationScreen::new);
    }
}
