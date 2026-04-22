package ttv.migami.jeg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ttv.migami.jeg.event.FactionEventTicker;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.fabric.FabricCreativeTabs;
import ttv.migami.jeg.fabric.FabricEntityInit;
import ttv.migami.jeg.fabric.FabricRecipeUnlock;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModCommands;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.NetworkHandler;
import net.neoforged.neoforge.common.NeoForge;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load();

        // Core content registration (the Fabric project reuses small NeoForge-style shims).
        ModEntities.REGISTER.register(NeoForge.EVENT_BUS);
        ModDataComponents.REGISTER.register(NeoForge.EVENT_BUS);
        ModSounds.REGISTER.register(NeoForge.EVENT_BUS);
        ModParticleTypes.REGISTER.register(NeoForge.EVENT_BUS);
        ModEffects.REGISTER.register(NeoForge.EVENT_BUS);
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

        // Commands and server lifecycle bridges.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> GunEvents.onServerStarted(new ServerStartedEvent(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> FactionEventTicker.onServerTick(new ServerTickEvent.Post(server)));

        // Bridge commonly used event-style handlers used by shared code.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                GunEvents.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(handler.player)));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            GunEvents.onPlayerJoinWorld(new EntityJoinLevelEvent(entity, level));
            GunnerMobSpawner.onEntityJoinWorld(new EntityJoinLevelEvent(entity, level));
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            for (var entity : serverLevel.getAllEntities()) {
                GunnerMobSpawner.onLivingUpdate(new EntityTickEvent.Post(entity));
            }
        });
    }
}
