package ttv.migami.jeg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.EntityJoinLevelEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.living.LivingIncomingDamageEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.entity.player.PlayerEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.server.ServerStartedEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.tick.ServerTickEvent;
import ttv.migami.jeg.event.AttachmentRuntimeEvents;
import ttv.migami.jeg.event.FactionEventTicker;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.fabric.FabricCreativeTabs;
import ttv.migami.jeg.fabric.FabricEntityInit;
import ttv.migami.jeg.fabric.FabricRecipeUnlock;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerFriendlyFireEvents;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModCommands;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.event.VehiclePassengerDamageEvents;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.common.NeoForge;

public final class FabricEntrypoint implements ModInitializer {
    @Override
    public void onInitialize() {
        Config.load();

        // Core content registration (the Fabric project reuses small NeoForge-style shims).
        ModBlocks.REGISTER.register(NeoForge.EVENT_BUS);
        ModBlockEntities.REGISTER.register(NeoForge.EVENT_BUS);
        ModMenuTypes.REGISTER.register(NeoForge.EVENT_BUS);
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
        ttv.migami.jeg.init.SpecialEquipmentDispenseBehaviors.register();
        VehicleDataManager.registerReloadListener();
        VehicleAssemblyRecipeManager.registerReloadListener();
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
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GunEvents.onPlayerLogin(new PlayerEvent.PlayerLoggedInEvent(handler.player));
            NetworkHandler.sendUiConfig(handler.player);
            NetworkHandler.sendVehicleData(handler.player);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(entity, source, amount);
            GunnerFriendlyFireEvents.onIncomingDamage(event);
            VehiclePassengerDamageEvents.onPassengerDamage(event);
            if (amount > 0.0F && !event.isCanceled()) {
                ttv.migami.jeg.item.DefuserItem.interruptIfDefusing(entity);
            }
            return !event.isCanceled();
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Armed bombers still detonate if killed mid-fuse.
            if (ttv.migami.jeg.faction.BomberGunnerHelper.isArmed(entity)
                    && !ttv.migami.jeg.faction.BomberGunnerHelper.hasDetonated(entity)) {
                ttv.migami.jeg.faction.BomberGunnerHelper.explodeVest(entity);
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            GunEvents.onPlayerJoinWorld(new EntityJoinLevelEvent(entity, level));
            GunnerMobSpawner.onEntityJoinWorld(new EntityJoinLevelEvent(entity, level));
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }
            for (var entity : serverLevel.getAllEntities()) {
                if (entity instanceof Player player) {
                    AttachmentRuntimeEvents.tickPlayer(player);
                }
                GunnerMobSpawner.onLivingUpdate(new EntityTickEvent.Post(entity));
                EnemyVehicleController.tickEntity(entity);
            }
        });
    }
}
