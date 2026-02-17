package ttv.migami.jeg;

import net.fabricmc.api.ModInitializer;
import ttv.migami.jeg.fabric.FabricCreativeTabs;
import ttv.migami.jeg.fabric.FabricEntityInit;
import ttv.migami.jeg.fabric.FabricRecipeUnlock;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.NetworkHandler;
import net.neoforged.neoforge.common.NeoForge;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        // Core content registration (the Fabric project reuses small NeoForge-style shims).
        ModEntities.REGISTER.register(NeoForge.EVENT_BUS);
        ModDataComponents.REGISTER.register(NeoForge.EVENT_BUS);
        ModSounds.REGISTER.register(NeoForge.EVENT_BUS);
        ModParticleTypes.REGISTER.register(NeoForge.EVENT_BUS);
        ModStructures.STRUCTURES.register(NeoForge.EVENT_BUS);
        ModStructures.PIECES.register(NeoForge.EVENT_BUS);
        // ModItems initializes spawn eggs which reference EntityTypes; register entities first.
        ModItems.REGISTER.register(NeoForge.EVENT_BUS);

        // Fabric-specific hooks.
        NetworkHandler.initCommon();
        FabricCreativeTabs.init();
        FabricRecipeUnlock.init();
        FabricEntityInit.init();

        // Runtime values.
        GunMobValues.init();
    }
}
