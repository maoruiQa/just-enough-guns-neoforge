package ttv.migami.jeg.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.GhoulRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomRenderer;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID + ".ClientSetup");

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        try {
            LOGGER.debug("Registering entity renderers for Just Enough Guns");

            event.registerEntityRenderer(ModEntities.GHOUL.get(), GhoulRenderer::new);
            event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
            event.registerEntityRenderer(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
            event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerRenderer::new);
            event.registerEntityRenderer(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomRenderer::new);
            event.registerEntityRenderer(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomRenderer::new);

            LOGGER.debug("Successfully registered {} entity renderers", 6);
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
