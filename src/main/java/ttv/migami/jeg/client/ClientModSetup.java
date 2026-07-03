package ttv.migami.jeg.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
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
        event.registerItem(new RepairToolClientExtensions(), ModItems.REPAIR_TOOL.get());
        event.registerItem(new VehicleAssemblingTableClientExtensions(), ModItems.VEHICLE_ASSEMBLING_TABLE.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ModItems.ATTACHMENTS.values().forEach(holder ->
                event.register((stack, tintIndex) -> tintIndex == 0 ? DyedItemColor.getOrDefault(stack, -1) : -1, holder.get())
        );
        registerGunnerSpawnEggColors(event);
        registerEnemyVehicleSpawnEggColors(event);
    }

    private static void registerGunnerSpawnEggColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                ClientModSetup::getSpawnEggColor,
                ModItems.GUNNER_SKELETON_SPAWN_EGG.get(),
                ModItems.GUNNER_ZOMBIE_SPAWN_EGG.get(),
                ModItems.GUNNER_GHOUL_SPAWN_EGG.get(),
                ModItems.GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get(),
                ModItems.GUNNER_PIGLIN_SPAWN_EGG.get(),
                ModItems.GUNNER_HUSK_SPAWN_EGG.get(),
                ModItems.GUNNER_WITHER_SKELETON_SPAWN_EGG.get(),
                ModItems.GUNNER_DROWNED_SPAWN_EGG.get(),
                ModItems.GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG.get(),
                ModItems.GUNNER_STRAY_SPAWN_EGG.get(),
                ModItems.GUNNER_PILLAGER_SPAWN_EGG.get(),
                ModItems.GUNNER_VINDICATOR_SPAWN_EGG.get(),
                ModItems.GUNNER_PIGLIN_BRUTE_SPAWN_EGG.get()
        );
    }

    private static int getSpawnEggColor(net.minecraft.world.item.ItemStack stack, int tintIndex) {
        return FastColor.ARGB32.opaque(((SpawnEggItem) stack.getItem()).getColor(tintIndex));
    }

    private static void registerEnemyVehicleSpawnEggColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> getSpawnEggColor(EntityType.PILLAGER, tintIndex),
                ModItems.ENEMY_LAV150_SPAWN_EGG.get(),
                ModItems.ENEMY_BMP2_SPAWN_EGG.get()
        );
        event.register(
                (stack, tintIndex) -> getSpawnEggColor(EntityType.PHANTOM, tintIndex),
                ModItems.ENEMY_AH6_SPAWN_EGG.get(),
                ModItems.ENEMY_MI28_SPAWN_EGG.get()
        );
    }

    private static int getSpawnEggColor(EntityType<?> type, int tintIndex) {
        return FastColor.ARGB32.opaque(SpawnEggItem.byId(type).getColor(tintIndex));
    }
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        GunDefinitions.ALL.keySet().forEach(id -> {
            event.register(new ModelResourceLocation(Reference.id("item/first_person/" + id.getPath()), "standalone"));
            event.register(new ModelResourceLocation(Reference.id("item/gui/" + id.getPath()), "standalone"));
        });
        event.register(new ModelResourceLocation(Reference.id("item/repair_tool_base"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("item/vehicle_assembling_table"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("special/holy_shotgun/main"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("special/holy_shotgun/pumpy"), "standalone"));
        event.register(new ModelResourceLocation(Reference.id("special/typhoonee/main"), "standalone"));
    }
}
