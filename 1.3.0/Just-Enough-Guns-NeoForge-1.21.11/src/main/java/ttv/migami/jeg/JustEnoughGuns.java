package ttv.migami.jeg;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModEntityEvents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModStructures;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.NetworkHandler;

@Mod(Reference.MOD_ID)
public final class JustEnoughGuns {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public JustEnoughGuns(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        LOGGER.info("Initializing Just Enough Guns for Minecraft 1.21.11");

        // Register mod content first
        ModItems.REGISTER.register(modBus);
        ModDataComponents.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
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
        NeoForge.EVENT_BUS.register(GunnerMobSpawner.class);

        LOGGER.debug("Game events registered successfully");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("JEG common setup started");

        try {
            // Initialize game values
            GunMobValues.init();

            LOGGER.info("JEG common setup completed successfully");

        } catch (Exception e) {
            LOGGER.error("JEG common setup failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ModItems.addToTab(event);
    }
}
