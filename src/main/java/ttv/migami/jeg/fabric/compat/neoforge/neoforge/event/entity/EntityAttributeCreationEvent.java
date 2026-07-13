package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class EntityAttributeCreationEvent {
    public <T extends net.minecraft.world.entity.LivingEntity> void put(EntityType<T> entityType, AttributeSupplier attributes) {
    }
}
