package ttv.migami.jeg.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
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

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        GunDefinitions.ALL.keySet().forEach(id -> {
            event.register(new ModelResourceLocation(Reference.id("item/first_person/" + id.getPath()), "standalone"));
            event.register(new ModelResourceLocation(Reference.id("item/gui/" + id.getPath()), "standalone"));
        });
        event.register(new ModelResourceLocation(Reference.id("special/holy_shotgun/main"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("special/holy_shotgun/pumpy"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("special/typhoonee/main"), "standalone"));
    }
}
