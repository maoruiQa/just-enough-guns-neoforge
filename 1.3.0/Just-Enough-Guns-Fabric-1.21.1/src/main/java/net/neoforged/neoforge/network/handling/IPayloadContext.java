package net.neoforged.neoforge.network.handling;

import net.minecraft.world.entity.player.Player;

public interface IPayloadContext {
    default void enqueueWork(Runnable runnable) {
        runnable.run();
    }

    Player player();
}
