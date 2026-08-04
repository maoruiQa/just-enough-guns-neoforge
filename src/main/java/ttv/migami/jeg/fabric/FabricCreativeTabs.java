package ttv.migami.jeg.fabric;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;

public final class FabricCreativeTabs {
    private FabricCreativeTabs() {}

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            ModItems.GUNS.forEach((id, holder) -> {
                if (!id.equals(ttv.migami.jeg.Reference.id("phantom_smg")) && !ModItems.isDisabledGunId(id)) {
                    entries.accept(holder.get());
                }
            });
            ModItems.AMMO.values().forEach(holder -> entries.accept(holder.get()));
            entries.accept(ModItems.DRONE.get());
            entries.accept(ModItems.MONITOR.get());
            entries.accept(ModItems.C4_BOMB.get());
            net.minecraft.world.item.ItemStack remoteC4 = new net.minecraft.world.item.ItemStack(ModItems.C4_BOMB.get());
            remoteC4.set(ttv.migami.jeg.init.ModDataComponents.C4_REMOTE.get(), true);
            entries.accept(remoteC4);
            entries.accept(ModItems.DETONATOR.get());
            entries.accept(ModItems.DEFUSER.get());
            entries.accept(ModItems.C4_VEST.get());
            entries.accept(ModItems.CLAYMORE_MINE.get());
            entries.accept(ModItems.TM_62.get());
            entries.accept(ModItems.MISSILE_ENGINE.get());
            entries.accept(ModItems.PISTOL_MAGAZINE.get());
            entries.accept(ModItems.SMG_MAGAZINE.get());
            entries.accept(ModItems.SMG_EXTENDED_MAGAZINE.get());
            entries.accept(ModItems.SMG_DRUM_MAGAZINE.get());
            entries.accept(ModItems.RIFLE_MAGAZINE.get());
            entries.accept(ModItems.RIFLE_EXTENDED_MAGAZINE.get());
            entries.accept(ModItems.RIFLE_DRUM_MAGAZINE.get());
            entries.accept(ModItems.SHOTGUN_MAGAZINE.get());
            entries.accept(ModItems.SHOTGUN_EXTENDED_MAGAZINE.get());
            entries.accept(ModItems.SHOTGUN_DRUM_MAGAZINE.get());
            entries.accept(ModItems.MACHINE_GUN_MAGAZINE.get());
            ModItems.ATTACHMENTS.values().forEach(holder -> entries.accept(holder.get()));
            entries.accept(ModItems.CLASSIC_SPRAY_CAN.get());
            entries.accept(ModItems.TOY_SPRAY_CAN.get());
            entries.accept(ModItems.WHITEOUT_SPRAY_CAN.get());
            entries.accept(ModItems.GOLDEN_SPRAY_CAN.get());
            entries.accept(ModItems.CREEPER_BIRTHDAY_PARTY_BADGE.get());
            entries.accept(ModItems.HEADPOPPER_BADGE.get());
            entries.accept(ModItems.TRICKSHOT_BADGE.get());
            ModItems.BULLETPROOF_HELMETS.values().forEach(holder -> entries.accept(holder.get()));
            ModItems.BULLETPROOF_VESTS.values().forEach(holder -> entries.accept(holder.get()));

            entries.accept(ModItems.PHANTOM_GUNNER_SPAWN_EGG.get());
            entries.accept(ModItems.TERROR_PHANTOM_SPAWN_EGG.get());
            entries.accept(ModItems.TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_LAV150_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_BMP2_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_AH6_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_MI28_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_SKELETON_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIE_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_HUSK_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(ModItems.GUNSMITH_MANUAL.get());
            entries.accept(ModItems.COOLANT.get());
            entries.accept(ModItems.ENHANCED_COOLANT.get());
            entries.accept(ModItems.VEHICLE_CONTAINER.get());
            entries.accept(ModItems.VEHICLE_ASSEMBLING_TABLE.get());
            entries.accept(ModItems.VEHICLE_CHARGING_STATION.get());
            entries.accept(ModItems.MAGAZINE_LOADER.get());
            VehicleDataManager.all().keySet().stream()
                    .sorted()
                    .map(VehicleContainerBlockEntity::createItemForVehicle)
                    .forEach(entries::accept);
            entries.accept(ModItems.CROWBAR.get());
            entries.accept(ModItems.REPAIR_KIT.get());
            entries.accept(ModItems.REPAIR_TOOL.get());
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(ModItems.PHANTOM_GUNNER_SPAWN_EGG.get());
            entries.accept(ModItems.TERROR_PHANTOM_SPAWN_EGG.get());
            entries.accept(ModItems.TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_LAV150_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_BMP2_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_AH6_SPAWN_EGG.get());
            entries.accept(ModItems.ENEMY_MI28_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_SKELETON_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIE_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_HUSK_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_DROWNED_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_STRAY_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PILLAGER_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_VINDICATOR_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_BRUTE_SPAWN_EGG.get());
        });
    }
}
