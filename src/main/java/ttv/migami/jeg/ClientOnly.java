package ttv.migami.jeg;

import ttv.migami.jeg.fabric.compat.neoforge.fml.loading.FMLEnvironment;

/**
 * Bridge class to safely invoke client-only code from common code
 * This prevents class loading issues on dedicated servers
 */
public class ClientOnly {

    /**
     * Initialize client-side setup only when running on the client
     * This method uses reflection to avoid loading client classes on servers
     */
    public static void initClient() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        try {
            // Use reflection to avoid loading ClientSetup class on servers
            Class<?> clientSetupClass = Class.forName("ttv.migami.jeg.client.ClientSetup");
            clientSetupClass.getDeclaredMethod("init").invoke(null);
            JustEnoughGuns.LOGGER.info("Client-side initialization completed");
        } catch (ClassNotFoundException ignored) {
            JustEnoughGuns.LOGGER.info("ClientSetup not found in current source set; using fallback client registrations");
        } catch (Exception e) {
            JustEnoughGuns.LOGGER.error("Failed to initialize client-side code: {}", e.getMessage(), e);
        }
    }
}
