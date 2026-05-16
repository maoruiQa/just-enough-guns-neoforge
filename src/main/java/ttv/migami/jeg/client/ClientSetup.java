package ttv.migami.jeg.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.GhoulRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerGeoRenderer;
import ttv.migami.jeg.client.render.entity.RaidEntityRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomGeoRenderer;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.vehicle.client.render.A10Renderer;
import ttv.migami.jeg.vehicle.client.render.Ah6Renderer;
import ttv.migami.jeg.vehicle.client.render.Bmp2Renderer;
import ttv.migami.jeg.vehicle.client.render.Hpj11Renderer;
import ttv.migami.jeg.vehicle.client.render.LaserTowerRenderer;
import ttv.migami.jeg.vehicle.client.render.Lav150Renderer;
import ttv.migami.jeg.vehicle.client.render.Mi28Renderer;
import ttv.migami.jeg.vehicle.client.render.SpeedboatRenderer;
import ttv.migami.jeg.vehicle.client.render.Tom6Renderer;
import ttv.migami.jeg.vehicle.client.render.TruckRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleDecoyRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleGeoRenderer;
import ttv.migami.jeg.vehicle.client.render.VehicleMissileRenderer;
import ttv.migami.jeg.vehicle.client.render.WaveforceTowerRenderer;
import ttv.migami.jeg.vehicle.client.render.block.VehicleAssemblingTableBlockEntityRenderer;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID + ".ClientSetup");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        try {
            LOGGER.debug("Registering entity renderers for Just Enough Guns");

            event.registerBlockEntityRenderer(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get(), VehicleAssemblingTableBlockEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.GHOUL.get(), GhoulRenderer::new);
            event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
            event.registerEntityRenderer(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.STUN_GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.SMOKE_GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.MOLOTOV_COCKTAIL.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.WATER_BOMB.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER_MINION.get(), PhantomGunnerGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.RAID_ENTITY.get(), RaidEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_WHEEL_VEHICLE.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.LIGHT_COMBAT_VEHICLE.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_HELICOPTER.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_BOAT.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_ARTILLERY.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TEST_AIRCRAFT.get(), VehicleGeoRenderer::new);
            event.registerEntityRenderer(ModEntities.TRUCK.get(), TruckRenderer::new);
            event.registerEntityRenderer(ModEntities.LAV150.get(), Lav150Renderer::new);
            event.registerEntityRenderer(ModEntities.SPEEDBOAT.get(), SpeedboatRenderer::new);
            event.registerEntityRenderer(ModEntities.AH6.get(), Ah6Renderer::new);
            event.registerEntityRenderer(ModEntities.A10.get(), A10Renderer::new);
            event.registerEntityRenderer(ModEntities.BMP2.get(), Bmp2Renderer::new);
            event.registerEntityRenderer(ModEntities.MI28.get(), Mi28Renderer::new);
            event.registerEntityRenderer(ModEntities.TOM6.get(), Tom6Renderer::new);
            event.registerEntityRenderer(ModEntities.LASER_TOWER.get(), LaserTowerRenderer::new);
            event.registerEntityRenderer(ModEntities.HPJ11.get(), Hpj11Renderer::new);
            event.registerEntityRenderer(ModEntities.WAVEFORCE_TOWER.get(), WaveforceTowerRenderer::new);
            event.registerEntityRenderer(ModEntities.VEHICLE_DECOY.get(), VehicleDecoyRenderer::new);
            event.registerEntityRenderer(ModEntities.VEHICLE_MISSILE.get(), VehicleMissileRenderer::new);

            LOGGER.debug("Successfully registered vehicle and gun entity renderers");
        } catch (Exception e) {
            LOGGER.error("Failed to register entity renderers", e);
            throw e;
        }
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        try {
            LOGGER.debug("Registering client extensions for gun items");

            int registeredCount = 0;
            for (var holder : ModItems.GUNS.values()) {
                event.registerItem(new GunItemClientExtensions(holder.get()), holder.get());
                registeredCount++;
            }

            LOGGER.debug("Successfully registered {} gun item client extensions", registeredCount);
        } catch (Exception e) {
            LOGGER.error("Failed to register client extensions", e);
            throw e;
        }
    }


    // NOTE: In NeoForge 1.21+, RegisterColorHandlersEvent.Item was replaced with RegisterColorHandlersEvent.ItemTintSources
    // This requires creating custom ItemTintSource implementations which is more complex.
    // Color data component and tooltip display are functional, visual rendering is disabled for now.
    // TODO: Implement ItemTintSource for dynamic color rendering in future update
}
