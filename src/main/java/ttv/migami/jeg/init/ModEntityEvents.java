package ttv.migami.jeg.init;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import ttv.migami.jeg.entity.GunnerEntity;
import ttv.migami.jeg.entity.monster.Ghoul;

public final class ModEntityEvents {
    private ModEntityEvents() {}

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUNNER.get(), GunnerEntity.createAttributes().build());
        event.put(ModEntities.GHOUL.get(), Ghoul.createAttributes().build());
    }

    public static void onSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.GHOUL.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Ghoul::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
