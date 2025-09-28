package net.neoforged.neoforge.event;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.LogicalSide;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Minimal compatibility shim for the legacy Forge {@code TickEvent} hierarchy.
 * <p>
 * NeoForge 1.21 replaced these events with more granular types; this bridge recreates the
 * old surface API so existing handlers continue to compile while the underlying bridge
 * forwards modern events (see {@code ttv.migami.jeg.util.bridge.LegacyTickBridge}).
 */
@SuppressWarnings("unused")
public abstract class TickEvent extends Event {
    public enum Phase {
        START,
        END
    }

    /** Logical side the tick fired on. */
    public final LogicalSide side;

    /** Start/end boundary of the tick. */
    public final Phase phase;

    protected TickEvent(LogicalSide side, Phase phase) {
        this.side = Objects.requireNonNull(side, "side");
        this.phase = Objects.requireNonNull(phase, "phase");
    }

    public static class ClientTickEvent extends TickEvent {
        public final Minecraft minecraft;

        public ClientTickEvent(Phase phase, Minecraft minecraft) {
            super(LogicalSide.CLIENT, phase);
            this.minecraft = minecraft;
        }
    }

    public static class RenderTickEvent extends TickEvent {
        public final float renderTickTime;

        public RenderTickEvent(Phase phase, float renderTickTime) {
            super(LogicalSide.CLIENT, phase);
            this.renderTickTime = renderTickTime;
        }
    }

    public static class PlayerTickEvent extends TickEvent {
        public final Player player;

        public PlayerTickEvent(Player player, LogicalSide side, Phase phase) {
            super(side, phase);
            this.player = Objects.requireNonNull(player, "player");
        }
    }

    public static class ServerTickEvent extends TickEvent {
        private final BooleanSupplier hasTime;
        private final MinecraftServer server;

        public ServerTickEvent(BooleanSupplier hasTime, MinecraftServer server, Phase phase) {
            super(LogicalSide.SERVER, phase);
            this.hasTime = Objects.requireNonNull(hasTime, "hasTime");
            this.server = Objects.requireNonNull(server, "server");
        }

        public boolean hasTime() {
            return hasTime.getAsBoolean();
        }

        public MinecraftServer getServer() {
            return server;
        }
    }
}
