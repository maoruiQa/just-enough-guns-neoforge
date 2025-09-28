package ttv.migami.jeg.event;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.Bubble;
import ttv.migami.jeg.entity.animal.Boo;
import ttv.migami.jeg.entity.monster.Ghoul;
import ttv.migami.jeg.entity.monster.phantom.gunner.PhantomGunner;
import ttv.migami.jeg.entity.monster.phantom.terror.TerrorPhantom;
import ttv.migami.jeg.init.ModEntities;

// @Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEventBus {

    @SubscribeEvent
    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GHOUL.get(), Ghoul.createAttributes().build());
        event.put(ModEntities.BOO.get(), Bee.createAttributes().build());
        event.put(ModEntities.TERROR_PHANMTOM.get(), TerrorPhantom.createAttributes().build());
        event.put(ModEntities.PHANTOM_GUNNER.get(), PhantomGunner.createAttributes().build());
        event.put(ModEntities.BUBBLE.get(), Bubble.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.GHOUL.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE,
                Ghoul::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
        event.register(
                ModEntities.BOO.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.WORLD_SURFACE,
                Boo::checkAnimalSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
    }
}
