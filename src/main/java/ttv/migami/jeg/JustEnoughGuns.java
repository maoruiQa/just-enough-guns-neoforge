package ttv.migami.jeg;

import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.client.FrameworkClientAPI;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;
import ttv.migami.jeg.block.ScrapBinBlock;
import ttv.migami.jeg.client.ClientHandler;
import ttv.migami.jeg.client.CustomGunManager;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.client.MetaLoader;
import ttv.migami.jeg.client.SpecialModels;
import ttv.migami.jeg.client.handler.CrosshairHandler;
import ttv.migami.jeg.client.render.entity.*;
import ttv.migami.jeg.common.BoundingBoxManager;
import ttv.migami.jeg.common.NetworkGunManager;
import ttv.migami.jeg.common.ProjectileManager;
import ttv.migami.jeg.datagen.*;
import ttv.migami.jeg.entity.ai.trumpet.TrumpetSkeletonAI;
import ttv.migami.jeg.entity.client.BubbleRenderer;
import ttv.migami.jeg.entity.client.PhantomGunnerRenderer;
import ttv.migami.jeg.entity.client.SplashRenderer;
import ttv.migami.jeg.entity.client.TerrorPhantomRenderer;
import ttv.migami.jeg.entity.projectile.*;
import ttv.migami.jeg.entity.throwable.GrenadeEntity;
import ttv.migami.jeg.event.ConfigPackLoader;
import ttv.migami.jeg.event.ModCommandsRegister;
import ttv.migami.jeg.event.ServerTickHandler;
import ttv.migami.jeg.faction.GunMobValues;
import ttv.migami.jeg.faction.GunnerMobSpawner;
import ttv.migami.jeg.init.*;
import ttv.migami.jeg.modifier.ModifierLoader;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.world.loot.ModLootModifiers;
import ttv.migami.jeg.util.ClientOnly;

import java.util.concurrent.CompletableFuture;

@Mod(Reference.MOD_ID)
public class JustEnoughGuns {
    public static boolean debugging = false;
    public static boolean controllableLoaded = false;
    public static boolean playerReviveLoaded = false;
    public static boolean yungsNetherFortLoaded = false;
    public static boolean valkyrienSkiesLoaded = false;
    public static boolean devilFruitsLoaded = false;
    public static boolean gunnersLoaded = false;
    public static boolean recruitsLoaded = false;
    public static boolean guardsLoaded = false;
    public static boolean shoulderSurfingLoaded = false;
    // I am not okay with the original mod's name haha
    public static boolean aQuietPlaceLoaded = false;
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public JustEnoughGuns(IEventBus modEventBus, ModContainer modContainer) {
        ConfigPackLoader.exportSampleResourcesIfMissing();
        ConfigPackLoader.exportSampleDataIfMissing();

        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.serverSpec);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new TrumpetSkeletonAI());
        NeoForge.EVENT_BUS.register(new GunnerMobSpawner());
        NeoForge.EVENT_BUS.register(new ModCommandsRegister());
        NeoForge.EVENT_BUS.register(ServerTickHandler.class);

        ModBlocks.REGISTER.register(modEventBus);
        ModContainers.REGISTER.register(modEventBus);
        ModEffects.REGISTER.register(modEventBus);
        ModEnchantments.REGISTER.register(modEventBus);
        ModEntities.REGISTER.register(modEventBus);
        ModItems.REGISTER.register(modEventBus);
        ModParticleTypes.REGISTER.register(modEventBus);
        ModRecipeSerializers.REGISTER.register(modEventBus);
        ModRecipeTypes.REGISTER.register(modEventBus);
        ModSounds.REGISTER.register(modEventBus);
        ModTileEntities.REGISTER.register(modEventBus);
        ModPointOfInterestTypes.REGISTER.register(modEventBus);
        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::gatherServerData);

        // OooOoOh spooky!
        GeckoLib.initialize();

        ClientOnly.run(() -> {
            FrameworkClientAPI.registerDataLoader(MetaLoader.getInstance());
            ClientHandler.onRegisterCreativeTab(modEventBus);
            modEventBus.addListener(ClientHandler::addPack);
            modEventBus.addListener(KeyBinds::registerKeyMappings);
            modEventBus.addListener(CrosshairHandler::onConfigReload);
            modEventBus.addListener(ClientHandler::onRegisterReloadListener);
            modEventBus.addListener(SpecialModels::onBake);
            modEventBus.addListener(this::onClientSetup);
        });

        controllableLoaded = ModList.get().isLoaded("controllable");
        playerReviveLoaded = ModList.get().isLoaded("playerrevive");
        valkyrienSkiesLoaded = ModList.get().isLoaded("valkyrienskies");
        yungsNetherFortLoaded = ModList.get().isLoaded("betterfortresses");
        devilFruitsLoaded = ModList.get().isLoaded("mdf");
        gunnersLoaded = ModList.get().isLoaded("gunners");
        recruitsLoaded = ModList.get().isLoaded("recruits");
        guardsLoaded = ModList.get().isLoaded("guardvillagers");
        shoulderSurfingLoaded = ModList.get().isLoaded("shouldersurfing");
        aQuietPlaceLoaded = ModList.get().isLoaded("death_angels");
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(new ModifierLoader());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
        {
            PacketHandler.init();
            ScrapBinBlock.bootStrap();
            GunMobValues.init();
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.AIMING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.RELOADING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.SHOOTING);
            FrameworkAPI.registerSyncedDataKey(ModSyncedDataKeys.BURST_COUNT);
            ProjectileManager.getInstance().registerFactory(Items.ARROW, (worldIn, entity, weapon, item, modifiedGun) -> new ArrowProjectileEntity(ModEntities.ARROW_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(Items.FIRE_CHARGE, (worldIn, entity, weapon, item, modifiedGun) -> new FlameProjectileEntity(ModEntities.FLAME_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.RIFLE_AMMO.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.PISTOL_AMMO.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.HANDMADE_SHELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SHOTGUN_SHELL.get(), (worldIn, entity, weapon, item, modifiedGun) -> new ProjectileEntity(ModEntities.PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.SPECTRE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new SpectreProjectileEntity(ModEntities.SPECTRE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.BLAZE_ROUND.get(), (worldIn, entity, weapon, item, modifiedGun) -> new BlazeProjectileEntity(ModEntities.BLAZE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(Items.ECHO_SHARD, (worldIn, entity, weapon, item, modifiedGun) -> new SonicProjectileEntity(ModEntities.SONIC_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(Items.EMERALD, (worldIn, entity, weapon, item, modifiedGun) -> new ResonanceProjectileEntity(ModEntities.RESONANCE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(Items.REDSTONE_TORCH, (worldIn, entity, weapon, item, modifiedGun) -> new BeamEntity(ModEntities.BEAM.get(), worldIn, entity, weapon, item, modifiedGun));

            ProjectileManager.getInstance().registerFactory(ModItems.FLARE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new FlareProjectileEntity(ModEntities.FLARE_PROJECTILE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.WATER_BOMB.get(), (worldIn, entity, weapon, item, modifiedGun) -> new WhirpoolEntity(ModEntities.WATER_BOMB.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.POCKET_BUBBLE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new PocketBubbleEntity(ModEntities.POCKET_BUBBLE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.GRENADE.get(), (worldIn, entity, weapon, item, modifiedGun) -> new GrenadeEntity(ModEntities.GRENADE.get(), worldIn, entity, weapon, item, modifiedGun));
            ProjectileManager.getInstance().registerFactory(ModItems.ROCKET.get(), (worldIn, entity, weapon, item, modifiedGun) -> new RocketEntity(ModEntities.ROCKET.get(), worldIn, entity, weapon, item, modifiedGun));
            if (Config.COMMON.gameplay.improvedHitboxes.get()) {
                NeoForge.EVENT_BUS.register(new BoundingBoxManager());
            }
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.RAID_ENTITY.get(), RaidEntityRenderer::new);
        EntityRenderers.register(ModEntities.TERROR_RAID_ENTITY.get(), TerrorRaidEntityRenderer::new);
        EntityRenderers.register(ModEntities.HEALING_TALISMAN.get(), ThrownItemRenderer::new);
        EntityRenderers.register(ModEntities.GHOUL.get(), GhoulRenderer::new);
        EntityRenderers.register(ModEntities.BOO.get(), BooRenderer::new);
        EntityRenderers.register(ModEntities.TERROR_PHANMTOM.get(), TerrorPhantomRenderer::new);
        EntityRenderers.register(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerRenderer::new);
        EntityRenderers.register(ModEntities.DYNAMIC_HELMET.get(), DynamicHelmetRenderer::new);
        EntityRenderers.register(ModEntities.SPLASH.get(), SplashRenderer::new);
        EntityRenderers.register(ModEntities.BUBBLE.get(), BubbleRenderer::new);
        event.enqueueWork(ClientHandler::setup);
    }

    private void gatherServerData(GatherDataEvent.Server event) {
        var generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        BlockTagGen blockTagGen = new BlockTagGen(output, lookupProvider);
        EntityTagGen entityTagGen = new EntityTagGen(output, lookupProvider);

        generator.addProvider(true, blockTagGen);
        generator.addProvider(true, entityTagGen);
        generator.addProvider(true, new ItemTagGen(output, lookupProvider, blockTagGen.contentsGetter()));
        generator.addProvider(true, new GunGen(output, lookupProvider));
        generator.addProvider(true, new CFGGunGen(output, lookupProvider));
        generator.addProvider(true, new WorldGen(output, lookupProvider));
        //generator.addProvider(true, new DamageTypeGen(output, lookupProvider));
        generator.addProvider(true, new ModifierGen(output, lookupProvider));
    }

    public static boolean isDebugging() {
        return false; //!FMLEnvironment.production;
    }
}
