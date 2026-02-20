package net.neoforged.neoforge.event.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.level.levelgen.Heightmap;

public class RegisterSpawnPlacementsEvent {
    public enum Operation {
        OR
    }

    @FunctionalInterface
    public interface SpawnPredicate<T extends net.minecraft.world.entity.Mob> {
        boolean test(EntityType<T> entityType, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnReason, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random);
    }

    public <T extends net.minecraft.world.entity.Mob> void register(
            EntityType<T> entityType,
            SpawnPlacementType placementType,
            Heightmap.Types heightmap,
            SpawnPredicate<T> predicate,
            Operation operation
    ) {
    }
}
