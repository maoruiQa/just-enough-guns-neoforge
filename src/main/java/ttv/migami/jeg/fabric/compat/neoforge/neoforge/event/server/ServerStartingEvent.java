package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.server;

import net.minecraft.server.MinecraftServer;

public class ServerStartingEvent {
    private final MinecraftServer server;

    public ServerStartingEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
