
package ttv.migami.jeg;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModEntityEvents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;

@Mod(Reference.MOD_ID)
public final class JustEnoughGuns {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public JustEnoughGuns(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);

        NeoForge.EVENT_BUS.register(GunEvents.class);

        ModItems.REGISTER.register(modBus);
        ModDataComponents.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onBuildCreativeTab);
        modBus.addListener(ModEntityEvents::onAttributeCreation);
        modBus.addListener(ModEntityEvents::onSpawnPlacement);

    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ModItems.addToTab(event);
    }
}
