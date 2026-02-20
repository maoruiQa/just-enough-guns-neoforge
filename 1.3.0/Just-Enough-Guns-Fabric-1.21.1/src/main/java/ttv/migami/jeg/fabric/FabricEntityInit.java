package ttv.migami.jeg.fabric;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.SpawnPlacements;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunnerMinion;
import ttv.migami.jeg.init.ModEntities;

public final class FabricEntityInit {
    private FabricEntityInit() {}

    public static void init() {
        FabricDefaultAttributeRegistry.register(ModEntities.PHANTOM_GUNNER.get(), PhantomGunner.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerMinion.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.TERROR_PHANTOM.get(), AbstractTerrorPhantom.createAttributes(1000.0D));
        FabricDefaultAttributeRegistry.register(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), AbstractTerrorPhantom.createAttributes(1200.0D));

        SpawnPlacements.register(
                ModEntities.PHANTOM_GUNNER.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules
        );
        SpawnPlacements.register(
                ModEntities.PHANTOM_GUNNER_MINION.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules
        );
        SpawnPlacements.register(
                ModEntities.TERROR_PHANTOM.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules
        );
        SpawnPlacements.register(
                ModEntities.TERROR_PHANTOM_GUARDIAN.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING,
                Mob::checkMobSpawnRules
        );
    }
}
