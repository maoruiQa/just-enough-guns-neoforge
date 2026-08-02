package ttv.migami.jeg;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttv.migami.jeg.event.AttachmentRuntimeEvents;
import ttv.migami.jeg.event.FactionEventTicker;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModEntityEvents;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.init.ModCommands;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.energy.VehicleEnergyStorage;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;

@Mod(Reference.MOD_ID)
public final class JustEnoughGuns {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public JustEnoughGuns(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        LOGGER.info("Initializing Just Enough Guns for Minecraft 1.21.11");

        // Register mod content first
        ModBlocks.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenuTypes.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModDataComponents.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModEffects.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModParticleTypes.REGISTER.register(modBus);
        ModStructures.STRUCTURES.register(modBus);
        ModStructures.PIECES.register(modBus);

        // Register lifecycle events
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);
        modBus.addListener(ModEntityEvents::onAttributeCreation);
        modBus.addListener(ModEntityEvents::onSpawnPlacement);
        modBus.addListener(NetworkHandler::register);
        modBus.addListener(this::registerCapabilities);

        // FIXED: Register game events immediately after mod content registration
        // This prevents timing issues with ModelManager.reload() in NeoForge 1.21.10
        registerGameEvents();

        LOGGER.info("JEG initialization completed successfully");
    }

    // FIXED: Remove the timing issue by registering events immediately
    private void registerGameEvents() {
        // Register game events immediately to ensure proper initialization order
        // This prevents the ModelManager.reload() NullPointerException
        NeoForge.EVENT_BUS.register(GunEvents.class);
        NeoForge.EVENT_BUS.register(AttachmentRuntimeEvents.class);
        NeoForge.EVENT_BUS.register(GunnerMobSpawner.class);
        NeoForge.EVENT_BUS.register(EnemyVehicleController.class);
        NeoForge.EVENT_BUS.register(FactionEventTicker.class);
        NeoForge.EVENT_BUS.addListener(ModCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(VehicleDataManager::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(VehicleAssemblyRecipeManager::onAddReloadListeners);

        // LOGGER.debug("Game events registered successfully");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("JEG common setup started");

        try {
            // Initialize game values
            GunMobValues.init();
            event.enqueueWork(ttv.migami.jeg.init.SpecialEquipmentDispenseBehaviors::register);

            LOGGER.info("JEG common setup completed successfully");

        } catch (Exception e) {
            LOGGER.error("JEG common setup failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerVehicleCapabilities(event, ModEntities.TEST_WHEEL_VEHICLE.get());
        registerVehicleCapabilities(event, ModEntities.LIGHT_COMBAT_VEHICLE.get());
        registerVehicleCapabilities(event, ModEntities.TEST_HELICOPTER.get());
        registerVehicleCapabilities(event, ModEntities.TEST_BOAT.get());
        registerVehicleCapabilities(event, ModEntities.TEST_ARTILLERY.get());
        registerVehicleCapabilities(event, ModEntities.TEST_AIRCRAFT.get());
        registerVehicleCapabilities(event, ModEntities.TRUCK.get());
        registerVehicleCapabilities(event, ModEntities.LAV150.get());
        registerVehicleCapabilities(event, ModEntities.SPEEDBOAT.get());
        registerVehicleCapabilities(event, ModEntities.AH6.get());
        registerVehicleCapabilities(event, ModEntities.A10.get());
        registerVehicleCapabilities(event, ModEntities.BMP2.get());
        registerVehicleCapabilities(event, ModEntities.MI28.get());
        registerVehicleCapabilities(event, ModEntities.TOM6.get());
        registerVehicleCapabilities(event, ModEntities.LASER_TOWER.get());
        registerVehicleCapabilities(event, ModEntities.HPJ11.get());
        registerVehicleCapabilities(event, ModEntities.WAVEFORCE_TOWER.get());
    }

    private static <T extends VehicleEntity> void registerVehicleCapabilities(RegisterCapabilitiesEvent event, EntityType<T> type) {
        event.registerEntity(Capabilities.EnergyStorage.ENTITY, type, (vehicle, side) -> new VehicleEnergyStorage(vehicle));
        event.registerEntity(Capabilities.ItemHandler.ENTITY, type, (vehicle, context) -> new InvWrapper(vehicle.vehicleInventory()));
        event.registerEntity(Capabilities.ItemHandler.ENTITY_AUTOMATION, type, (vehicle, side) -> new InvWrapper(vehicle.vehicleInventory()));
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ModItems.addToTab(event);
    }
}
