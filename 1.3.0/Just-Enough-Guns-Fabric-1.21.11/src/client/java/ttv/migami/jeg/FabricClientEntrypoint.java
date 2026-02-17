package ttv.migami.jeg;

import net.fabricmc.api.ClientModInitializer;
import ttv.migami.jeg.client.FabricClientBootstrap;

public final class FabricClientEntrypoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricClientBootstrap.init();
    }
}
