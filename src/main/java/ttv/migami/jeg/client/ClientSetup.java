package ttv.migami.jeg.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.entity.BulletRenderer;
import ttv.migami.jeg.client.render.entity.GhoulRenderer;
import ttv.migami.jeg.client.render.entity.GunnerRenderer;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUNNER.get(), GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.GHOUL.get(), GhoulRenderer::new);
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.GRENADE.get(), context -> new ThrownItemRenderer<>(context, 1.0F, true));
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ModItems.GUNS.values().forEach(holder -> event.registerItem(new GunItemClientExtensions(holder.get()), holder.get()));
    }
}
