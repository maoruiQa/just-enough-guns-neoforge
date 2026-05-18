package ttv.migami.jeg.init;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.event.RecipeUnlockHandler;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.ArmoredJoyHarnessItem;
import ttv.migami.jeg.item.BulletproofArmorItem;
import ttv.migami.jeg.item.EnhancedCoolantItem;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.GunnerSpawnEggItem;
import ttv.migami.jeg.item.JoyousArmorPlateItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.ManualItem;
import ttv.migami.jeg.item.ModSpawnEggItem;
import ttv.migami.jeg.item.MolotovCocktailItem;
import ttv.migami.jeg.item.SmokeGrenadeItem;
import ttv.migami.jeg.item.StunGrenadeItem;
import ttv.migami.jeg.item.WaterBombItem;
import ttv.migami.jeg.item.DescribedAmmoItem;
import ttv.migami.jeg.item.RepairToolItem;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;
import ttv.migami.jeg.vehicle.item.VehicleContainerItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(Registries.ITEM, Reference.MOD_ID);

    public static final Map<Identifier, DeferredHolder<Item, Item>> AMMO = new LinkedHashMap<>();
    public static final DeferredHolder<Item, VehicleContainerItem> VEHICLE_CONTAINER = REGISTER.register(
            "vehicle_container",
            () -> new VehicleContainerItem(ModBlocks.VEHICLE_CONTAINER.get(), baseProperties(Reference.id("vehicle_container")).stacksTo(1))
    );
    public static final DeferredHolder<Item, VehicleAssemblingTableBlockItem> VEHICLE_ASSEMBLING_TABLE = REGISTER.register(
            "vehicle_assembling_table",
            () -> new VehicleAssemblingTableBlockItem(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(), baseProperties(Reference.id("vehicle_assembling_table")).stacksTo(64))
    );
    public static final DeferredHolder<Item, BlockItem> VEHICLE_CHARGING_STATION = REGISTER.register(
            "vehicle_charging_station",
            () -> new BlockItem(ModBlocks.VEHICLE_CHARGING_STATION.get(), baseProperties(Reference.id("vehicle_charging_station")).stacksTo(64))
    );
    public static final DeferredHolder<Item, Item> CROWBAR = REGISTER.register(
            "crowbar",
            () -> new Item(baseProperties(Reference.id("crowbar")).stacksTo(1).durability(128))
    );
    public static final DeferredHolder<Item, Item> REPAIR_KIT = REGISTER.register(
            "repair_kit",
            () -> new Item(baseProperties(Reference.id("repair_kit")).stacksTo(16))
    );
    public static final DeferredHolder<Item, RepairToolItem> REPAIR_TOOL = REGISTER.register(
            "repair_tool",
            () -> new RepairToolItem(baseProperties(Reference.id("repair_tool")))
    );
    public static final DeferredHolder<Item, Item> COOLANT = REGISTER.register("coolant", () -> new Item(baseProperties(Reference.id("coolant")).stacksTo(1)));
    public static final DeferredHolder<Item, EnhancedCoolantItem> ENHANCED_COOLANT = REGISTER.register("enhanced_coolant", () -> new EnhancedCoolantItem(baseProperties(Reference.id("enhanced_coolant")).stacksTo(1)));
    public static final DeferredHolder<Item, Item> MISSILE_ENGINE = REGISTER.register(
            "missile_engine",
            () -> new Item(baseProperties(Reference.id("missile_engine")).stacksTo(64))
    );
    public static final DeferredHolder<Item, MagazineItem> PISTOL_MAGAZINE = REGISTER.register(
            "pistol_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("pistol_magazine")).stacksTo(1), MagazineItem.MagazineType.PISTOL)
    );
    public static final DeferredHolder<Item, MagazineItem> SMG_MAGAZINE = REGISTER.register(
            "smg_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("smg_magazine")).stacksTo(1), MagazineItem.MagazineType.SMG)
    );
    public static final DeferredHolder<Item, MagazineItem> RIFLE_MAGAZINE = REGISTER.register(
            "rifle_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("rifle_magazine")).stacksTo(1), MagazineItem.MagazineType.RIFLE)
    );
    public static final DeferredHolder<Item, MagazineItem> SHOTGUN_MAGAZINE = REGISTER.register(
            "shotgun_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("shotgun_magazine")).stacksTo(1), MagazineItem.MagazineType.SHOTGUN)
    );
    public static final DeferredHolder<Item, MagazineItem> MACHINE_GUN_MAGAZINE = REGISTER.register(
            "machine_gun_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("machine_gun_magazine")).stacksTo(1), MagazineItem.MagazineType.MACHINE_GUN)
    );
    public static final Map<Identifier, DeferredHolder<Item, GunItem>> GUNS = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_DIAMOND = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_NETHERITE = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_HELMETS = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_VESTS = new LinkedHashMap<>();
    private static final List<ResourceKey<Recipe<?>>> MANUAL_RECIPES;
    private static final Identifier PHANTOM_SMG_ID = Reference.id("phantom_smg");

    private static final Set<String> AMMO_IDS = Set.of(
            "pistol_ammo",
            "rifle_ammo",
            "small_shell",
            "autocannon_shell",
            "shotgun_shell",
            "handmade_shell",
            "spectre_round",
            "blaze_round",
            "rocket",
            "small_rocket",
            "medium_anti_air_missile",
            "medium_anti_ground_missile",
            "large_anti_ground_missile",
            "grenade",
            "stun_grenade",
            "smoke_grenade",
            "molotov_cocktail",
            "water_bomb",
            "flare"
    );

    static {
        registerAmmoItems();
        registerGunItems();
        registerArmoredHarnessItems();
        registerBulletproofArmorItems();
        MANUAL_RECIPES = buildManualRecipes();
    }

    public static final DeferredHolder<Item, ManualItem> GUNSMITH_MANUAL = REGISTER.register(
            "gunsmith_manual",
            () -> new ManualItem(baseProperties(Reference.id("gunsmith_manual")).stacksTo(1), MANUAL_RECIPES)
    );

    public static final DeferredHolder<Item, SpawnEggItem> PHANTOM_GUNNER_SPAWN_EGG = REGISTER.register(
            "phantom_gunner_spawn_egg",
            () -> new ModSpawnEggItem(ModEntities.PHANTOM_GUNNER.get(), baseProperties(Reference.id("phantom_gunner_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, SpawnEggItem> TERROR_PHANTOM_SPAWN_EGG = REGISTER.register(
            "terror_phantom_spawn_egg",
            () -> new ModSpawnEggItem(ModEntities.TERROR_PHANTOM.get(), baseProperties(Reference.id("terror_phantom_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, SpawnEggItem> TERROR_PHANTOM_GUARDIAN_SPAWN_EGG = REGISTER.register(
            "terror_phantom_guardian_spawn_egg",
            () -> new ModSpawnEggItem(ModEntities.TERROR_PHANTOM_GUARDIAN.get(), baseProperties(Reference.id("terror_phantom_guardian_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_SKELETON_SPAWN_EGG = REGISTER.register(
            "gunner_skeleton_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.SKELETON,
                    baseProperties(Reference.id("gunner_skeleton_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIE_SPAWN_EGG = REGISTER.register(
            "gunner_zombie_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIE,
                    baseProperties(Reference.id("gunner_zombie_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG = REGISTER.register(
            "gunner_zombified_piglin_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIFIED_PIGLIN,
                    baseProperties(Reference.id("gunner_zombified_piglin_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PIGLIN_SPAWN_EGG = REGISTER.register(
            "gunner_piglin_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PIGLIN,
                    baseProperties(Reference.id("gunner_piglin_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_HUSK_SPAWN_EGG = REGISTER.register(
            "gunner_husk_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.HUSK,
                    baseProperties(Reference.id("gunner_husk_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PARCHED_SPAWN_EGG = REGISTER.register(
            "gunner_parched_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PARCHED,
                    baseProperties(Reference.id("gunner_parched_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_WITHER_SKELETON_SPAWN_EGG = REGISTER.register(
            "gunner_wither_skeleton_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.WITHER_SKELETON,
                    baseProperties(Reference.id("gunner_wither_skeleton_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_DROWNED_SPAWN_EGG = REGISTER.register(
            "gunner_drowned_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.DROWNED,
                    baseProperties(Reference.id("gunner_drowned_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG = REGISTER.register(
            "gunner_zombie_villager_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIE_VILLAGER,
                    baseProperties(Reference.id("gunner_zombie_villager_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_STRAY_SPAWN_EGG = REGISTER.register(
            "gunner_stray_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.STRAY,
                    baseProperties(Reference.id("gunner_stray_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PILLAGER_SPAWN_EGG = REGISTER.register(
            "gunner_pillager_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PILLAGER,
                    baseProperties(Reference.id("gunner_pillager_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_VINDICATOR_SPAWN_EGG = REGISTER.register(
            "gunner_vindicator_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.VINDICATOR,
                    baseProperties(Reference.id("gunner_vindicator_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PIGLIN_BRUTE_SPAWN_EGG = REGISTER.register(
            "gunner_piglin_brute_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PIGLIN_BRUTE,
                    baseProperties(Reference.id("gunner_piglin_brute_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, SpawnEggItem> GUNNER_GHOUL_SPAWN_EGG = REGISTER.register(
            "gunner_ghoul_spawn_egg",
            () -> new ModSpawnEggItem(ModEntities.GHOUL.get(), baseProperties(Reference.id("gunner_ghoul_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, JoyousArmorPlateItem> JOYOUS_ARMOR_PLATE = REGISTER.register(
            "joyous_armor_plate",
            () -> new JoyousArmorPlateItem(baseProperties(Reference.id("joyous_armor_plate")))
    );

    private static void registerAmmoItems() {
        for (String path : AMMO_IDS) {
            Identifier id = Reference.id(path);
            switch (path) {
                case "grenade" -> AMMO.put(id, REGISTER.register(path, () -> new GrenadeItem(baseProperties(id).stacksTo(16))));
                case "stun_grenade" -> AMMO.put(id, REGISTER.register(path, () -> new StunGrenadeItem(baseProperties(id).stacksTo(16))));
                case "smoke_grenade" -> AMMO.put(id, REGISTER.register(path, () -> new SmokeGrenadeItem(baseProperties(id).stacksTo(16))));
                case "molotov_cocktail" -> AMMO.put(id, REGISTER.register(path, () -> new MolotovCocktailItem(baseProperties(id).stacksTo(16))));
                case "water_bomb" -> AMMO.put(id, REGISTER.register(path, () -> new WaterBombItem(baseProperties(id).stacksTo(16))));
                case "medium_anti_air_missile", "medium_anti_ground_missile", "large_anti_ground_missile" ->
                        AMMO.put(id, REGISTER.register(path, () -> new DescribedAmmoItem(baseProperties(id), "tooltip.jeg." + path)));
                default -> AMMO.put(id, REGISTER.register(path, () -> new Item(baseProperties(id))));
            }
        }
    }

    private static void registerGunItems() {
        for (Map.Entry<Identifier, GunStats> entry : GunDefinitions.ALL.entrySet()) {
            Identifier id = entry.getKey();
            GunStats stats = entry.getValue();
            DeferredHolder<Item, GunItem> holder = REGISTER.register(id.getPath(), () -> new AnimatedGunItem(defaultGunProperties(id, stats), stats));
            GUNS.put(id, holder);
        }
    }

    private static void registerArmoredHarnessItems() {
        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getName();

            Identifier baseId = Reference.id("armored_joy_harness_" + colorName);
            DeferredHolder<Item, ArmoredJoyHarnessItem> baseHolder = REGISTER.register(
                    "armored_joy_harness_" + colorName,
                    () -> new ArmoredJoyHarnessItem(color,
                            ArmoredJoyHarnessItem.buildProperties(baseProperties(baseId), color, ArmoredJoyHarnessItem.HarnessTier.BASE),
                            30.0F,
                            100.0F,
                            null,
                            ArmoredJoyHarnessItem.HarnessTier.BASE)
            );
            ARMORED_JOY_HARNESSES.put(color, baseHolder);

            Identifier diamondId = Reference.id("armored_joy_harness_" + colorName + "_diamond");
            DeferredHolder<Item, ArmoredJoyHarnessItem> diamondHolder = REGISTER.register(
                    diamondId.getPath(),
                    () -> new ArmoredJoyHarnessItem(color,
                            ArmoredJoyHarnessItem.buildProperties(baseProperties(diamondId), color, ArmoredJoyHarnessItem.HarnessTier.DIAMOND),
                            40.0F, 160.0F,
                            Component.translatable("tooltip.jeg.harness_material.diamond"),
                            ArmoredJoyHarnessItem.HarnessTier.DIAMOND)
            );
            ARMORED_JOY_HARNESSES_DIAMOND.put(color, diamondHolder);

            Identifier netheriteId = Reference.id("armored_joy_harness_" + colorName + "_netherite");
            DeferredHolder<Item, ArmoredJoyHarnessItem> netheriteHolder = REGISTER.register(
                    netheriteId.getPath(),
                    () -> new ArmoredJoyHarnessItem(color,
                            ArmoredJoyHarnessItem.buildProperties(baseProperties(netheriteId), color, ArmoredJoyHarnessItem.HarnessTier.NETHERITE),
                            50.0F, 250.0F,
                            Component.translatable("tooltip.jeg.harness_material.netherite"),
                            ArmoredJoyHarnessItem.HarnessTier.NETHERITE)
            );
            ARMORED_JOY_HARNESSES_NETHERITE.put(color, netheriteHolder);
        }
    }

    private static void registerBulletproofArmorItems() {
        for (BulletproofArmorItem.Tier tier : BulletproofArmorItem.Tier.values()) {
            Identifier helmetId = Reference.id("bulletproof_helmet_" + tier.suffix());
            DeferredHolder<Item, BulletproofArmorItem> helmet = REGISTER.register(
                    helmetId.getPath(),
                    () -> new BulletproofArmorItem(tier, EquipmentSlot.HEAD, baseProperties(helmetId))
            );
            BULLETPROOF_HELMETS.put(tier, helmet);

            Identifier vestId = Reference.id("bulletproof_vest_" + tier.suffix());
            DeferredHolder<Item, BulletproofArmorItem> vest = REGISTER.register(
                    vestId.getPath(),
                    () -> new BulletproofArmorItem(tier, EquipmentSlot.CHEST, baseProperties(vestId))
            );
            BULLETPROOF_VESTS.put(tier, vest);
        }
    }

    private static List<ResourceKey<Recipe<?>>> buildManualRecipes() {
        java.util.ArrayList<ResourceKey<Recipe<?>>> keys = new java.util.ArrayList<>();
        for (String ammo : AMMO_IDS) {
            if ("water_bomb".equals(ammo)) {
                continue;
            }
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id(ammo)));
        }
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("enhanced_coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("missile_engine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("pistol_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("smg_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("rifle_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("shotgun_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("machine_gun_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_assembling_table")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_charging_station")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("crowbar")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_kit")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_tool")));
        for (Identifier id : GunDefinitions.ALL.keySet()) {
            if (!id.equals(PHANTOM_SMG_ID)) {
                keys.add(ResourceKey.create(Registries.RECIPE, id));
            }
        }
        RecipeUnlockHandler.BULLETPROOF_RECIPES.forEach(keys::add);

        for (DyeColor color : DyeColor.values()) {
            String name = color.getName();
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("armored_joy_harness_" + name)));
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("armored_joy_harness_" + name + "_diamond")));
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("armored_joy_harness_" + name + "_netherite")));
        }
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("joyous_armor_plate")));
        return List.copyOf(keys);
    }

    public static List<ResourceKey<Recipe<?>>> manualRecipes() {
        return unlockGunRecipeKeys();
    }

    public static List<ResourceKey<Recipe<?>>> unlockGunRecipeKeys() {
        return MANUAL_RECIPES;
    }

    private static Item.Properties defaultGunProperties(Identifier id, GunStats stats) {
        int baseDurability = switch (stats.reloadType()) {
            case "jeg:inventory_fed" -> 1024;
            case "jeg:manual" -> 256;
            default -> 512;
        };
        int durability = Math.max(1, (int) Math.round(baseDurability * 3.75D));
        return baseProperties(id).stacksTo(1).durability(durability).repairable(net.minecraft.world.item.Items.IRON_INGOT);
    }

    private static Item.Properties baseProperties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            GUNS.forEach((id, holder) -> {
                if (!id.equals(PHANTOM_SMG_ID)) {
                    event.accept(holder.get());
                }
            });
            AMMO.values().forEach(holder -> event.accept(holder.get()));
            event.accept(PISTOL_MAGAZINE.get());
            event.accept(SMG_MAGAZINE.get());
            event.accept(RIFLE_MAGAZINE.get());
            event.accept(SHOTGUN_MAGAZINE.get());
            event.accept(MACHINE_GUN_MAGAZINE.get());
            event.accept(MISSILE_ENGINE.get());
            BULLETPROOF_HELMETS.values().forEach(holder -> event.accept(holder.get()));
            BULLETPROOF_VESTS.values().forEach(holder -> event.accept(holder.get()));
            event.accept(PHANTOM_GUNNER_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            event.accept(GUNNER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_SPAWN_EGG.get());
            event.accept(GUNNER_HUSK_SPAWN_EGG.get());
            event.accept(GUNNER_PARCHED_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
        }

        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(GUNSMITH_MANUAL.get());
            event.accept(COOLANT.get());
            event.accept(ENHANCED_COOLANT.get());
            event.accept(VEHICLE_ASSEMBLING_TABLE.get());
            event.accept(VEHICLE_CHARGING_STATION.get());
            event.accept(VEHICLE_CONTAINER.get());
            event.accept(CROWBAR.get());
            event.accept(REPAIR_KIT.get());
            event.accept(REPAIR_TOOL.get());
            ARMORED_JOY_HARNESSES.values().forEach(holder -> event.accept(holder.get()));
            ARMORED_JOY_HARNESSES_DIAMOND.values().forEach(holder -> event.accept(holder.get()));
            ARMORED_JOY_HARNESSES_NETHERITE.values().forEach(holder -> event.accept(holder.get()));
            event.accept(JOYOUS_ARMOR_PLATE.get());
        }

        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(PHANTOM_GUNNER_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            event.accept(GUNNER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_SPAWN_EGG.get());
            event.accept(GUNNER_HUSK_SPAWN_EGG.get());
            event.accept(GUNNER_PARCHED_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_DROWNED_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG.get());
            event.accept(GUNNER_STRAY_SPAWN_EGG.get());
            event.accept(GUNNER_PILLAGER_SPAWN_EGG.get());
            event.accept(GUNNER_VINDICATOR_SPAWN_EGG.get());
            event.accept(GUNNER_PIGLIN_BRUTE_SPAWN_EGG.get());
            event.accept(GUNNER_GHOUL_SPAWN_EGG.get());
        }
    }
}
