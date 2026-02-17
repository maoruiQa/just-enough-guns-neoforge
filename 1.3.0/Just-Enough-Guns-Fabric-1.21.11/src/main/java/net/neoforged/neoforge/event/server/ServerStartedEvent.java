package net.neoforged.neoforge.event.server;

import net.minecraft.server.MinecraftServer;

public class ServerStartedEvent {
    private final MinecraftServer server;

    public ServerStartedEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
