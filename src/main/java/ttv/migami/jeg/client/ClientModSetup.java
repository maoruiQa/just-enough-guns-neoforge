package ttv.migami.jeg.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModItems;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModSetup {
    private ClientModSetup() {}

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (var holder : ModItems.GUNS.values()) {
            event.registerItem(new GunItemClientExtensions(holder.get()), holder.get());
        }
    }
}
