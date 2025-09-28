package ttv.migami.jeg.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.client.ModModelLayers;
import ttv.migami.jeg.entity.client.SplashModel;

// @Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEventsBus {
    @SubscribeEvent
    public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.SPLASH, SplashModel::createBodyLayer);
    }
}
