package net.neoforged.neoforge.event.tick;

import net.minecraft.world.entity.Entity;

public final class EntityTickEvent {
    private EntityTickEvent() {
    }

    public static class Post {
        private final Entity entity;

        public Post(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
