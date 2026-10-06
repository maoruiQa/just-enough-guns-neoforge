package ttv.migami.jeg;

import net.fabricmc.api.ClientModInitializer;
import ttv.migami.jeg.client.FabricClientBootstrap;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientBootstrap.init();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> ttv.migami.jeg.vehicle.client.VehicleCaptureCheck.serverTick());
    }
}