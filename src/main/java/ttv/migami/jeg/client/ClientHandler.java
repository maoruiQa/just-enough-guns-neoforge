package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.client.handler.BulletTrailRenderingHandler;
import ttv.migami.jeg.client.handler.CrosshairHandler;
import ttv.migami.jeg.client.handler.FlashlightHandler;
import ttv.migami.jeg.client.handler.InspectHandler;
import ttv.migami.jeg.client.handler.MeleeHandler;
import ttv.migami.jeg.client.handler.PlayerModelHandler;
import ttv.migami.jeg.client.handler.ReloadHandler;
import ttv.migami.jeg.client.handler.ShootingHandler;
import ttv.migami.jeg.client.handler.SoundHandler;
import ttv.migami.jeg.client.render.gun.ModelOverrides;
import ttv.migami.jeg.client.render.gun.model.*;
import ttv.migami.jeg.client.screen.AttachmentScreen;
import ttv.migami.jeg.client.screen.BlueprintWorkbenchScreen;
import ttv.migami.jeg.client.screen.GunmetalWorkbenchScreen;
import ttv.migami.jeg.client.screen.GunniteWorkbenchScreen;
import ttv.migami.jeg.client.screen.SchematicStationScreen;
import ttv.migami.jeg.client.screen.ScrapWorkbenchScreen;
import ttv.migami.jeg.client.screen.recycler.RecyclerScreen;
import ttv.migami.jeg.common.NetworkGunManager;
import ttv.migami.jeg.debug.IEditorMenu;
import ttv.migami.jeg.debug.client.screen.EditorScreen;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModContainers;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.impl.Attachment;
import ttv.migami.jeg.client.util.PropertyHelperReloadListener;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageAttachments;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Author: MrCrayfish
 */
public final class ClientHandler
{
    private ClientHandler() {}

    public static void addPack(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.CLIENT_RESOURCES)
        {
            return;
        }

        Path root = NetworkGunManager.CONFIG_GUN_DIR      // …/config/jeg/guns
                .getParent()                         // …/config/jeg
                .resolve("guns")                     // (safety)
                .resolve("assets");                  // …/config/jeg/guns/assets

        if (!Files.isDirectory(root))
        {
            return;
        }
        // NeoForge does not yet provide a drop-in replacement for Forge's PathPackResources helper.
        // The data-driven guns still load via the datapack channel, so we can safely skip injecting
        // an extra resource pack here for now.
    }

    public static void setup()
    {
        NeoForge.EVENT_BUS.register(AimingHandler.get());
        NeoForge.EVENT_BUS.register(BulletTrailRenderingHandler.get());
        NeoForge.EVENT_BUS.register(CrosshairHandler.get());
        NeoForge.EVENT_BUS.register(FlashlightHandler.get());
        NeoForge.EVENT_BUS.register(MeleeHandler.get());
        NeoForge.EVENT_BUS.register(InspectHandler.get());
        NeoForge.EVENT_BUS.register(ReloadHandler.get());
        NeoForge.EVENT_BUS.register(ShootingHandler.get());
        NeoForge.EVENT_BUS.register(SoundHandler.get());
        NeoForge.EVENT_BUS.register(new PlayerModelHandler());
        NeoForge.EVENT_BUS.addListener(Attachment::addInformationEvent);

        setupRenderLayers();
        registerModelOverrides();
        registerScreenFactories();
    }

    private static void setupRenderLayers()
    {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SCRAP_WORKBENCH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GUNMETAL_WORKBENCH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GUNNITE_WORKBENCH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SCHEMATIC_STATION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BLUEPRINT_WORKBENCH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.RECYCLER.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.AMMO_BOX.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.DYNAMIC_LIGHT.get(), RenderType.translucent());
    }

    private static void registerModelOverrides()
    {
        ModelOverrides.register(ModItems.COMBAT_RIFLE.get(), new CombatRifleModel());
        ModelOverrides.register(ModItems.ASSAULT_RIFLE.get(), new AssaultRifleModel());
        ModelOverrides.register(ModItems.REVOLVER.get(), new RevolverModel());
        ModelOverrides.register(ModItems.WATERPIPE_SHOTGUN.get(), new WaterpipeShotgunModel());
        ModelOverrides.register(ModItems.SEMI_AUTO_RIFLE.get(), new SemiAutoRifleModel());
        ModelOverrides.register(ModItems.BURST_RIFLE.get(), new BurstRifleModel());
        ModelOverrides.register(ModItems.PUMP_SHOTGUN.get(), new PumpShotgunModel());
        ModelOverrides.register(ModItems.BOLT_ACTION_RIFLE.get(), new BoltActionRifleModel());
        ModelOverrides.register(ModItems.CUSTOM_SMG.get(), new CustomSMGModel());
        ModelOverrides.register(ModItems.BLOSSOM_RIFLE.get(), new BlossomRifleModel());
        ModelOverrides.register(ModItems.DOUBLE_BARREL_SHOTGUN.get(), new DoubleBarrelShotgunModel());
        ModelOverrides.register(ModItems.HOLY_SHOTGUN.get(), new HolyShotgunModel());
        ModelOverrides.register(ModItems.ATLANTEAN_SPEAR.get(), new AtlanteanSpearModel());
        ModelOverrides.register(ModItems.TYPHOONEE.get(), new TyphooneeModel());
        ModelOverrides.register(ModItems.FLARE_GUN.get(), new FlareGunModel());
        ModelOverrides.register(ModItems.HOLLENFIRE_MK2.get(), new HollenfireMK2Model());
        ModelOverrides.register(ModItems.SOULHUNTER_MK2.get(), new SoulhunterMK2Model());
        ModelOverrides.register(ModItems.REPEATING_SHOTGUN.get(), new RepeatingShotgunModel());
        ModelOverrides.register(ModItems.INFANTRY_RIFLE.get(), new InfantryRifleModel());
        ModelOverrides.register(ModItems.SERVICE_RIFLE.get(), new ServiceRifleModel());
        ModelOverrides.register(ModItems.ROCKET_LAUNCHER.get(), new RocketLauncherModel());
        ModelOverrides.register(ModItems.PRIMITIVE_BOW.get(), new PrimitiveBowModel());
        ModelOverrides.register(ModItems.COMPOUND_BOW.get(), new CompoundBowModel());
        ModelOverrides.register(ModItems.LIGHT_MACHINE_GUN.get(), new LightMachineGunModel());
        ModelOverrides.register(ModItems.GRENADE_LAUNCHER.get(), new GrenadeLauncherModel());
        ModelOverrides.register(ModItems.SUPERSONIC_SHOTGUN.get(), new SupersonicShotgunModel());
        ModelOverrides.register(ModItems.SUBSONIC_RIFLE.get(), new SubsonicRifleModel());
        ModelOverrides.register(ModItems.HYPERSONIC_CANNON.get(), new HypersonicCannonModel());
        ModelOverrides.register(ModItems.FLAMETHROWER.get(), new FlamethrowerModel());
        ModelOverrides.register(ModItems.SEMI_AUTO_PISTOL.get(), new SemiAutoPistolModel());
        ModelOverrides.register(ModItems.COMBAT_PISTOL.get(), new CombatPistolModel());
        ModelOverrides.register(ModItems.MINIGUN.get(), new MinigunModel());
        ModelOverrides.register(ModItems.GRENADE_LAUNCHER.get(), new GrenadeLauncherModel());
        ModelOverrides.register(ModItems.SUPERSONIC_SHOTGUN.get(), new SupersonicShotgunModel());
        ModelOverrides.register(ModItems.SUBSONIC_RIFLE.get(), new SubsonicRifleModel());
        ModelOverrides.register(ModItems.HYPERSONIC_CANNON.get(), new HypersonicCannonModel());
        ModelOverrides.register(ModItems.MINIGUN.get(), new MinigunModel());
        ModelOverrides.register(ModItems.FLARE_GUN.get(), new FlareGunModel());
        ModelOverrides.register(Items.WOODEN_SWORD, new BayonetWoodenModel());
        ModelOverrides.register(Items.STONE_SWORD, new BayonetStoneModel());
        ModelOverrides.register(Items.IRON_SWORD, new BayonetIronModel());
        ModelOverrides.register(Items.GOLDEN_SWORD, new BayonetGoldenModel());
        ModelOverrides.register(Items.DIAMOND_SWORD, new BayonetDiamondModel());
        ModelOverrides.register(Items.NETHERITE_SWORD, new BayonetNetheriteModel());
    }

    private static void registerScreenFactories()
    {
        MenuScreens.register(ModContainers.GUNNITE_WORKBENCH.get(), GunniteWorkbenchScreen::new);
        MenuScreens.register(ModContainers.SCRAP_WORKBENCH.get(), ScrapWorkbenchScreen::new);
        MenuScreens.register(ModContainers.GUNMETAL_WORKBENCH.get(), GunmetalWorkbenchScreen::new);
        MenuScreens.register(ModContainers.ATTACHMENTS.get(), AttachmentScreen::new);
        MenuScreens.register(ModContainers.RECYCLER.get(), RecyclerScreen::new);
        MenuScreens.register(ModContainers.SCHEMATIC_STATION.get(), SchematicStationScreen::new);
        MenuScreens.register(ModContainers.BLUEPRINT_WORKBENCH.get(), BlueprintWorkbenchScreen::new);
        MenuScreens.register(ModContainers.AMMO_BOX.get(), ttv.migami.jeg.client.screen.AmmoBoxScreen::new);
    }

    public static void onRegisterCreativeTab(IEventBus bus)
    {
        // Recreate minimal creative tab registration until a full port is ready.
        DeferredRegister<CreativeModeTab> register = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Reference.MOD_ID);
        register.register("creative_tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Reference.MOD_ID))
                .icon(() -> new ItemStack(ModItems.ASSAULT_RIFLE.get()))
                .displayItems((parameters, output) -> ModItems.REGISTER.getEntries().forEach(entry -> output.accept(entry.get())) )
                .build());
        register.register(bus);
    }

    public static Screen createEditorScreen(IEditorMenu menu)
    {
        return new EditorScreen(Minecraft.getInstance().screen, menu);
    }

    public static void onRegisterReloadListener(AddClientReloadListenersEvent event)
    {
        event.addListener(Reference.id("property_helper_cache"), new PropertyHelperReloadListener());
    }

    public static void openAttachmentScreen()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
        {
            PacketHandler.getPlayChannel().sendToServer(new C2SMessageAttachments());
        }
    }

    private static boolean isSpawnEgg(Object item)
    {
        return item instanceof SpawnEggItem;
    }
}
