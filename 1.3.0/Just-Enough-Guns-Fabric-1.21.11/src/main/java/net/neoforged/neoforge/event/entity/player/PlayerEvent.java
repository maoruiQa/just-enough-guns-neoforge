package net.neoforged.neoforge.event.entity.player;

import net.minecraft.world.entity.player.Player;

public class PlayerEvent {
    private final Player entity;

    protected PlayerEvent(Player entity) {
        this.entity = entity;
    }

    public Player getEntity() {
        return entity;
    }

    public static class PlayerLoggedInEvent extends PlayerEvent {
        public PlayerLoggedInEvent(Player entity) {
            super(entity);
        }
    }
}
