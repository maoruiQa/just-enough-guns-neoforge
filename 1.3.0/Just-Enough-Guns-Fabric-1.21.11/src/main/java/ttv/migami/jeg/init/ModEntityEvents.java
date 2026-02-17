package ttv.migami.jeg.init;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import ttv.migami.jeg.entity.monster.Ghoul;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunnerMinion;

public final class ModEntityEvents {
    private ModEntityEvents() {}

    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GHOUL.get(), Ghoul.createAttributes().build());
        event.put(ModEntities.PHANTOM_GUNNER.get(), PhantomGunner.createAttributes().build());
        event.put(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerMinion.createAttributes().build());
        event.put(ModEntities.TERROR_PHANTOM.get(), AbstractTerrorPhantom.createAttributes(1000.0D).build());
        event.put(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), AbstractTerrorPhantom.createAttributes(1200.0D).build());
    }

    public static void onSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.GHOUL.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Ghoul::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );

        event.register(
                ModEntities.PHANTOM_GUNNER.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );

        event.register(
                ModEntities.PHANTOM_GUNNER_MINION.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );

        event.register(
                ModEntities.TERROR_PHANTOM.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );

        event.register(
                ModEntities.TERROR_PHANTOM_GUARDIAN.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR
        );
    }
}
