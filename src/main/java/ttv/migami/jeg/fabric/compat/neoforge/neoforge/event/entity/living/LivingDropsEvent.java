package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class LivingDropsEvent {
    private final LivingEntity entity;
    private final List<ItemEntity> drops = new ArrayList<>();

    public LivingDropsEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public List<ItemEntity> getDrops() {
        return drops;
    }
}
