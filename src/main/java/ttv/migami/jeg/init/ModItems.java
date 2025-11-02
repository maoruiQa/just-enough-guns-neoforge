package ttv.migami.jeg.init;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.event.RecipeUnlockHandler;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.GunnerSpawnEggItem;
import ttv.migami.jeg.item.ModSpawnEggItem;
import ttv.migami.jeg.item.ManualItem;
import ttv.migami.jeg.item.ArmoredJoyHarnessItem;
import ttv.migami.jeg.item.BulletproofArmorItem;
import ttv.migami.jeg.item.JoyousArmorPlateItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(Registries.ITEM, Reference.MOD_ID);

    public static final Map<ResourceLocation, DeferredHolder<Item, Item>> AMMO = new LinkedHashMap<>();
    public static final Map<ResourceLocation, DeferredHolder<Item, GunItem>> GUNS = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_DIAMOND = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_NETHERITE = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_HELMETS = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_VESTS = new LinkedHashMap<>();
    private static final List<ResourceKey<Recipe<?>>> MANUAL_RECIPES;
    private static final ResourceLocation PHANTOM_SMG_ID = Reference.id("phantom_smg");

    private static final Set<String> AMMO_IDS = Set.of(
            "pistol_ammo",
            "rifle_ammo",
            "shotgun_shell",
            "handmade_shell",
            "spectre_round",
            "blaze_round",
            "rocket",
            "grenade",
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
            ResourceLocation id = Reference.id(path);
            if ("grenade".equals(path)) {
                AMMO.put(id, REGISTER.register(path, () -> new GrenadeItem(baseProperties(id).stacksTo(16))));
            } else {
                AMMO.put(id, REGISTER.register(path, () -> new Item(baseProperties(id))));
            }
        }
    }

    private static void registerGunItems() {
        for (Map.Entry<ResourceLocation, GunStats> entry : GunDefinitions.ALL.entrySet()) {
            ResourceLocation id = entry.getKey();
            GunStats stats = entry.getValue();
            DeferredHolder<Item, GunItem> holder = REGISTER.register(id.getPath(), () -> new GunItem(defaultGunProperties(id, stats), stats));
            GUNS.put(id, holder);
        }
    }

    private static void registerArmoredHarnessItems() {
        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getName();

            ResourceLocation baseId = Reference.id("armored_joy_harness_" + colorName);
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

            ResourceLocation diamondId = Reference.id("armored_joy_harness_" + colorName + "_diamond");
            DeferredHolder<Item, ArmoredJoyHarnessItem> diamondHolder = REGISTER.register(
                    diamondId.getPath(),
                    () -> new ArmoredJoyHarnessItem(color,
                            ArmoredJoyHarnessItem.buildProperties(baseProperties(diamondId), color, ArmoredJoyHarnessItem.HarnessTier.DIAMOND),
                            40.0F, 160.0F,
                            Component.translatable("tooltip.jeg.harness_material.diamond"),
                            ArmoredJoyHarnessItem.HarnessTier.DIAMOND)
            );
            ARMORED_JOY_HARNESSES_DIAMOND.put(color, diamondHolder);

            ResourceLocation netheriteId = Reference.id("armored_joy_harness_" + colorName + "_netherite");
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
            ResourceLocation helmetId = Reference.id("bulletproof_helmet_" + tier.suffix());
            DeferredHolder<Item, BulletproofArmorItem> helmet = REGISTER.register(
                    helmetId.getPath(),
                    () -> new BulletproofArmorItem(tier, EquipmentSlot.HEAD, baseProperties(helmetId))
            );
            BULLETPROOF_HELMETS.put(tier, helmet);

            ResourceLocation vestId = Reference.id("bulletproof_vest_" + tier.suffix());
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
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id(ammo)));
        }
        for (ResourceLocation id : GunDefinitions.ALL.keySet()) {
            if (!id.equals(PHANTOM_SMG_ID)) {
                keys.add(ResourceKey.create(Registries.RECIPE, id));
            }
        }
        RecipeUnlockHandler.BULLETPROOF_RECIPES.forEach(keys::add);

        // Add all colored harness recipes
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
        return MANUAL_RECIPES;
    }

    private static Item.Properties defaultGunProperties(ResourceLocation id, GunStats stats) {
        int durability = switch (stats.reloadType()) {
            case "jeg:inventory_fed" -> 1024;
            case "jeg:manual" -> 256;
            default -> 512;
        };
        return baseProperties(id).stacksTo(1).durability(durability).repairable(net.minecraft.world.item.Items.IRON_INGOT);
    }

    private static Item.Properties baseProperties(ResourceLocation id) {
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
            BULLETPROOF_HELMETS.values().forEach(holder -> event.accept(holder.get()));
            BULLETPROOF_VESTS.values().forEach(holder -> event.accept(holder.get()));
            event.accept(PHANTOM_GUNNER_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            event.accept(GUNNER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_SPAWN_EGG.get());
            event.accept(GUNNER_HUSK_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
        }

        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(GUNSMITH_MANUAL.get());
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
