package ttv.migami.jeg.fabric.compat.neoforge.neoforge.client.event;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class EntityRenderersEvent {
    private EntityRenderersEvent() {
    }

    public static class RegisterRenderers {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        }
    }
}
