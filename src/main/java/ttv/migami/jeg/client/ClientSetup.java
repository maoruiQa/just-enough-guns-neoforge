package ttv.migami.jeg.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.GhoulRenderer;
import ttv.migami.jeg.client.render.entity.GunnerRenderer;
import ttv.migami.jeg.client.render.entity.PhantomGunnerRenderer;
import ttv.migami.jeg.client.render.entity.TerrorPhantomRenderer;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft == null) {
                return;
            }
            net.minecraft.server.packs.resources.ResourceManager manager = minecraft.getResourceManager();
            for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
                net.minecraft.resources.ResourceLocation modelId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ttv.migami.jeg.Reference.MOD_ID, "models/item/armored_joy_harness_" + color.getName() + ".json");
                boolean present = manager.getResource(modelId).isPresent();
                ttv.migami.jeg.JustEnoughGuns.LOGGER.info("[ClientSetup] Harness model {} present? {}", modelId, present);
            }
            net.minecraft.resources.ResourceLocation plateModel = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(ttv.migami.jeg.Reference.MOD_ID, "models/item/joyous_armor_plate.json");
            boolean platePresent = manager.getResource(plateModel).isPresent();
            ttv.migami.jeg.JustEnoughGuns.LOGGER.info("[ClientSetup] Joyous armor plate model {} present? {}", plateModel, platePresent);
        });
    }

    
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUNNER.get(), GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
        event.registerEntityRenderer(ModEntities.PHANTOM_GUNNER.get(), PhantomGunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.TERROR_PHANTOM.get(), TerrorPhantomRenderer::new);
        event.registerEntityRenderer(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), TerrorPhantomRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ModItems.GUNS.values().forEach(holder -> event.registerItem(new GunItemClientExtensions(holder.get()), holder.get()));
    }

    // NOTE: In NeoForge 1.21+, RegisterColorHandlersEvent.Item was replaced with RegisterColorHandlersEvent.ItemTintSources
    // This requires creating custom ItemTintSource implementations which is more complex.
    // Color data component and tooltip display are functional, visual rendering is disabled for now.
    // TODO: Implement ItemTintSource for dynamic color rendering in future update
}

