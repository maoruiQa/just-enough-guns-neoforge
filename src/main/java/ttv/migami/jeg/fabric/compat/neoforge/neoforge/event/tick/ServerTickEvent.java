package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.tick;

import net.minecraft.server.MinecraftServer;

public final class ServerTickEvent {
    private ServerTickEvent() {
    }

    public static class Post {
        private final MinecraftServer server;

        public Post() {
            this.server = null;
        }

        public Post(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }
}
