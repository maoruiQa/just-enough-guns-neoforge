package ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnGroupData;

public class FinalizeSpawnEvent {
    private final Entity entity;
    private final DifficultyInstance difficulty;
    private final Object spawnType;
    private final SpawnGroupData spawnData;

    public FinalizeSpawnEvent(Entity entity, DifficultyInstance difficulty, Object spawnType, SpawnGroupData spawnData) {
        this.entity = entity;
        this.difficulty = difficulty;
        this.spawnType = spawnType;
        this.spawnData = spawnData;
    }

    public Entity getEntity() {
        return entity;
    }

    public DifficultyInstance getDifficulty() {
        return difficulty;
    }

    public Object getSpawnType() {
        return spawnType;
    }

    public SpawnGroupData getSpawnData() {
        return spawnData;
    }
}
