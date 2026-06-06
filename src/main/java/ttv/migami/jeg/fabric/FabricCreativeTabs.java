package ttv.migami.jeg.fabric;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.Reference;

public final class FabricCreativeTabs {
    private FabricCreativeTabs() {}

    public static void init() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(entries -> {
            ModItems.GUNS.forEach((id, holder) -> {
                if (!id.equals(Reference.id("phantom_smg"))) {
                    entries.accept(holder.get());
                }
            });
            ModItems.AMMO.values().forEach(holder -> entries.accept(holder.get()));
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
            entries.accept(ModItems.MISSILE_ENGINE.get());
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
            entries.accept(ModItems.GUNNER_PARCHED_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(ModItems.GUNSMITH_MANUAL.get());
            entries.accept(ModItems.COOLANT.get());
            entries.accept(ModItems.ENHANCED_COOLANT.get());
            entries.accept(ModItems.VEHICLE_ASSEMBLING_TABLE.get());
            entries.accept(ModItems.VEHICLE_CHARGING_STATION.get());
            entries.accept(ModItems.VEHICLE_CONTAINER.get());
            entries.accept(ModItems.CROWBAR.get());
            entries.accept(ModItems.REPAIR_KIT.get());
            entries.accept(ModItems.REPAIR_TOOL.get());
            ModItems.ARMORED_JOY_HARNESSES.values().forEach(holder -> entries.accept(holder.get()));
            ModItems.ARMORED_JOY_HARNESSES_DIAMOND.values().forEach(holder -> entries.accept(holder.get()));
            ModItems.ARMORED_JOY_HARNESSES_NETHERITE.values().forEach(holder -> entries.accept(holder.get()));
        });

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
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
            entries.accept(ModItems.GUNNER_PARCHED_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_DROWNED_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_STRAY_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PILLAGER_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_VINDICATOR_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_PIGLIN_BRUTE_SPAWN_EGG.get());
            entries.accept(ModItems.GUNNER_GHOUL_SPAWN_EGG.get());
        });
    }
}

