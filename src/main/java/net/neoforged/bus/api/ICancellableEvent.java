package net.neoforged.bus.api;

/**
 * Minimal cancellable event contract for legacy Forge-style events.
 */
public interface ICancellableEvent {
    boolean isCanceled();
    void setCanceled(boolean canceled);
}
