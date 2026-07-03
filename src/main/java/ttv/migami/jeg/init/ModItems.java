package ttv.migami.jeg.init;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AttachmentItem;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.event.RecipeUnlockHandler;
import ttv.migami.jeg.item.attachment.AttachmentModifiers;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.KillEffectItem;
import ttv.migami.jeg.item.attachment.PaintJobCanItem;
import ttv.migami.jeg.item.EnhancedCoolantItem;
import ttv.migami.jeg.item.GrenadeItem;
import ttv.migami.jeg.item.AnimatedGunItem;
import ttv.migami.jeg.item.DescribedAmmoItem;
import ttv.migami.jeg.item.EnemyVehicleSpawnItem;
import ttv.migami.jeg.item.FlashlightAttachmentItem;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.GunnerSpawnEggItem;
import ttv.migami.jeg.item.MagazineItem;
import ttv.migami.jeg.item.MolotovCocktailItem;
import ttv.migami.jeg.item.ModSpawnEggItem;
import ttv.migami.jeg.item.ManualItem;
import ttv.migami.jeg.item.BulletproofArmorItem;
import ttv.migami.jeg.item.RepairKitItem;
import ttv.migami.jeg.item.RepairToolItem;
import ttv.migami.jeg.item.SmokeGrenadeItem;
import ttv.migami.jeg.item.StunGrenadeItem;
import ttv.migami.jeg.item.WaterBombItem;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;
import ttv.migami.jeg.vehicle.item.VehicleContainerItem;
// import ttv.migami.jeg.item.ArmoredJoyHarnessItem;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(Registries.ITEM, Reference.MOD_ID);

    public static final Map<ResourceLocation, DeferredHolder<Item, Item>> AMMO = new LinkedHashMap<>();
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
    public static final DeferredHolder<Item, BlockItem> MAGAZINE_LOADER = REGISTER.register(
            "magazine_loader",
            () -> new BlockItem(ModBlocks.MAGAZINE_LOADER.get(), baseProperties(Reference.id("magazine_loader")).stacksTo(64))
    );
    public static final DeferredHolder<Item, Item> CROWBAR = REGISTER.register(
            "crowbar",
            () -> new Item(baseProperties(Reference.id("crowbar")).stacksTo(1).durability(128))
    );
    public static final DeferredHolder<Item, Item> REPAIR_KIT = REGISTER.register(
            "repair_kit",
            () -> new RepairKitItem(baseProperties(Reference.id("repair_kit")).stacksTo(16))
    );
    public static final DeferredHolder<Item, RepairToolItem> REPAIR_TOOL = REGISTER.register(
            "repair_tool",
            RepairToolItem::new
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
    public static final DeferredHolder<Item, MagazineItem> SMG_EXTENDED_MAGAZINE = REGISTER.register(
            "smg_extended_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("smg_extended_magazine")).stacksTo(1), MagazineItem.MagazineType.SMG_EXTENDED)
    );
    public static final DeferredHolder<Item, MagazineItem> SMG_DRUM_MAGAZINE = REGISTER.register(
            "smg_drum_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("smg_drum_magazine")).stacksTo(1), MagazineItem.MagazineType.SMG_DRUM)
    );
    public static final DeferredHolder<Item, MagazineItem> RIFLE_MAGAZINE = REGISTER.register(
            "rifle_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("rifle_magazine")).stacksTo(1), MagazineItem.MagazineType.RIFLE)
    );
    public static final DeferredHolder<Item, MagazineItem> RIFLE_EXTENDED_MAGAZINE = REGISTER.register(
            "rifle_extended_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("rifle_extended_magazine")).stacksTo(1), MagazineItem.MagazineType.RIFLE_EXTENDED)
    );
    public static final DeferredHolder<Item, MagazineItem> RIFLE_DRUM_MAGAZINE = REGISTER.register(
            "rifle_drum_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("rifle_drum_magazine")).stacksTo(1), MagazineItem.MagazineType.RIFLE_DRUM)
    );
    public static final DeferredHolder<Item, MagazineItem> SHOTGUN_MAGAZINE = REGISTER.register(
            "shotgun_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("shotgun_magazine")).stacksTo(1), MagazineItem.MagazineType.SHOTGUN)
    );
    public static final DeferredHolder<Item, MagazineItem> SHOTGUN_EXTENDED_MAGAZINE = REGISTER.register(
            "shotgun_extended_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("shotgun_extended_magazine")).stacksTo(1), MagazineItem.MagazineType.SHOTGUN_EXTENDED)
    );
    public static final DeferredHolder<Item, MagazineItem> SHOTGUN_DRUM_MAGAZINE = REGISTER.register(
            "shotgun_drum_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("shotgun_drum_magazine")).stacksTo(1), MagazineItem.MagazineType.SHOTGUN_DRUM)
    );
    public static final DeferredHolder<Item, MagazineItem> MACHINE_GUN_MAGAZINE = REGISTER.register(
            "machine_gun_magazine",
            () -> new MagazineItem(baseProperties(Reference.id("machine_gun_magazine")).stacksTo(1), MagazineItem.MagazineType.MACHINE_GUN)
    );
    public static final Map<ResourceLocation, DeferredHolder<Item, AttachmentItem>> ATTACHMENTS = new LinkedHashMap<>();
    public static final DeferredHolder<Item, AttachmentItem> REFLEX_SIGHT = registerAttachment(
            "reflex_sight",
            AttachmentType.SCOPE,
            AttachmentModifiers.builder().aimFovModifier(0.7F).adsSpeedMultiplier(0.95D).adsViewOffset(0.0D, 0.65D, -1.2D).build(),
            baseProperties(Reference.id("reflex_sight")).stacksTo(1).durability(800)
    );
    public static final DeferredHolder<Item, AttachmentItem> MONOCLE_SIGHT = registerAttachment(
            "monocle_sight",
            AttachmentType.SCOPE,
            AttachmentModifiers.builder().aimFovModifier(0.7F).adsSpeedMultiplier(0.95D).adsViewOffset(0.0D, 0.65D, -1.2D).build(),
            baseProperties(Reference.id("monocle_sight")).stacksTo(1).durability(800)
    );
    public static final DeferredHolder<Item, AttachmentItem> HOLOGRAPHIC_SIGHT = registerAttachment(
            "holographic_sight",
            AttachmentType.SCOPE,
            AttachmentModifiers.builder().aimFovModifier(0.5F).adsSpeedMultiplier(0.95D).adsViewOffset(0.0D, 0.75D, -1.6D).build(),
            baseProperties(Reference.id("holographic_sight")).stacksTo(1).durability(800).rarity(Rarity.UNCOMMON)
    );
    public static final DeferredHolder<Item, AttachmentItem> TELESCOPIC_SIGHT = registerAttachment(
            "telescopic_sight",
            AttachmentType.SCOPE,
            AttachmentModifiers.builder().aimFovModifier(0.2F).adsSpeedMultiplier(0.76D).adsViewOffset(0.0D, 1.0D, -3.15D).build(),
            baseProperties(Reference.id("telescopic_sight")).stacksTo(1).durability(800).rarity(Rarity.EPIC)
    );
    public static final DeferredHolder<Item, AttachmentItem> MAKESHIFT_STOCK = registerAttachment(
            "makeshift_stock",
            AttachmentType.STOCK,
            AttachmentModifiers.builder().recoilMultiplier(0.7F).kickMultiplier(0.7F).build(),
            baseProperties(Reference.id("makeshift_stock")).stacksTo(1).durability(300)
    );
    public static final DeferredHolder<Item, AttachmentItem> LIGHT_STOCK = registerAttachment(
            "light_stock",
            AttachmentType.STOCK,
            AttachmentModifiers.builder().recoilMultiplier(0.4F).kickMultiplier(0.9F).spreadMultiplier(0.75F).adsSpeedMultiplier(0.95D).build(),
            baseProperties(Reference.id("light_stock")).stacksTo(1).durability(600)
    );
    public static final DeferredHolder<Item, AttachmentItem> TACTICAL_STOCK = registerAttachment(
            "tactical_stock",
            AttachmentType.STOCK,
            AttachmentModifiers.builder().recoilMultiplier(0.5F).kickMultiplier(0.4F).spreadMultiplier(0.5F).adsSpeedMultiplier(0.9D).build(),
            baseProperties(Reference.id("tactical_stock")).stacksTo(1).durability(800)
    );
    public static final DeferredHolder<Item, AttachmentItem> WEIGHTED_STOCK = registerAttachment(
            "weighted_stock",
            AttachmentType.STOCK,
            AttachmentModifiers.builder().recoilMultiplier(0.4F).kickMultiplier(0.3F).spreadMultiplier(0.25F).adsSpeedMultiplier(0.9D).build(),
            baseProperties(Reference.id("weighted_stock")).stacksTo(1).durability(1000)
    );
    public static final DeferredHolder<Item, AttachmentItem> SILENCER = registerAttachment(
            "silencer",
            AttachmentType.BARREL,
            AttachmentModifiers.builder().silenced().fireSoundRadiusMultiplier(0.25D).build(),
            baseProperties(Reference.id("silencer")).stacksTo(1).durability(500)
    );
    public static final DeferredHolder<Item, AttachmentItem> EXPLOSIVE_MUZZLE = registerAttachment(
            "explosive_muzzle",
            AttachmentType.BARREL,
            AttachmentModifiers.builder().explosiveAmmo().increasedJamming().damageMultiplier(1.1F).recoilMultiplier(1.5F).kickMultiplier(1.35F).spreadMultiplier(1.3F).adsSpeedMultiplier(0.9D).build(),
            baseProperties(Reference.id("explosive_muzzle")).stacksTo(1).durability(64).rarity(Rarity.UNCOMMON)
    );
    public static final DeferredHolder<Item, AttachmentItem> TRUMPET = registerAttachment(
            "trumpet",
            AttachmentType.BARREL,
            AttachmentModifiers.builder().annoying().build(),
            baseProperties(Reference.id("trumpet")).stacksTo(1).durability(64).rarity(Rarity.RARE)
    );
    public static final DeferredHolder<Item, AttachmentItem> LIGHT_GRIP = registerAttachment(
            "light_grip",
            AttachmentType.UNDER_BARREL,
            AttachmentModifiers.builder().recoilMultiplier(0.75F).kickMultiplier(0.75F).spreadMultiplier(0.8F).adsSpeedMultiplier(1.2D).build(),
            baseProperties(Reference.id("light_grip")).stacksTo(1).durability(600)
    );
    public static final DeferredHolder<Item, AttachmentItem> VERTICAL_GRIP = registerAttachment(
            "vertical_grip",
            AttachmentType.UNDER_BARREL,
            AttachmentModifiers.builder().recoilMultiplier(0.65F).kickMultiplier(0.65F).spreadMultiplier(0.5F).adsSpeedMultiplier(0.95D).build(),
            baseProperties(Reference.id("vertical_grip")).stacksTo(1).durability(800)
    );
    public static final DeferredHolder<Item, AttachmentItem> ANGLED_GRIP = registerAttachment(
            "angled_grip",
            AttachmentType.UNDER_BARREL,
            AttachmentModifiers.builder().recoilMultiplier(0.65F).kickMultiplier(0.65F).spreadMultiplier(0.5F).adsSpeedMultiplier(0.95D).build(),
            baseProperties(Reference.id("angled_grip")).stacksTo(1).durability(800)
    );
    public static final DeferredHolder<Item, AttachmentItem> EXTENDED_MAG = registerAttachment(
            "extended_mag",
            AttachmentType.MAGAZINE,
            AttachmentModifiers.builder().adsSpeedMultiplier(0.95D).magazineCapacityMultiplier(1.5D).build(),
            baseProperties(Reference.id("extended_mag")).stacksTo(1)
    );
    public static final DeferredHolder<Item, AttachmentItem> DRUM_MAG = registerAttachment(
            "drum_mag",
            AttachmentType.MAGAZINE,
            AttachmentModifiers.builder().adsSpeedMultiplier(0.9D).magazineCapacityMultiplier(2.0D).build(),
            baseProperties(Reference.id("drum_mag")).stacksTo(1)
    );
    public static final DeferredHolder<Item, AttachmentItem> FLASHLIGHT = registerAttachment(
            "flashlight",
            () -> new FlashlightAttachmentItem(
                    AttachmentType.SPECIAL,
                    AttachmentModifiers.builder().flashlight().adsSpeedMultiplier(0.95D).build(),
                    baseProperties(Reference.id("flashlight")).stacksTo(1).rarity(Rarity.UNCOMMON)
            )
    );
    public static final DeferredHolder<Item, AttachmentItem> LASER_POINTER = registerAttachment(
            "laser_pointer",
            AttachmentType.SPECIAL,
            AttachmentModifiers.builder().laserPointer().recoilMultiplier(0.4F).kickMultiplier(0.9F).spreadMultiplier(0.75F).adsSpeedMultiplier(0.95D).build(),
            baseProperties(Reference.id("laser_pointer")).stacksTo(1).rarity(Rarity.UNCOMMON)
    );
    public static final DeferredHolder<Item, PaintJobCanItem> CLASSIC_SPRAY_CAN = REGISTER.register(
            "classic_spray_can",
            () -> new PaintJobCanItem(baseProperties(Reference.id("classic_spray_can")).stacksTo(1).rarity(Rarity.UNCOMMON), "classic")
    );
    public static final DeferredHolder<Item, PaintJobCanItem> TOY_SPRAY_CAN = REGISTER.register(
            "toy_spray_can",
            () -> new PaintJobCanItem(baseProperties(Reference.id("toy_spray_can")).stacksTo(1).rarity(Rarity.RARE), "toy")
    );
    public static final DeferredHolder<Item, PaintJobCanItem> WHITEOUT_SPRAY_CAN = REGISTER.register(
            "whiteout_spray_can",
            () -> new PaintJobCanItem(baseProperties(Reference.id("whiteout_spray_can")).stacksTo(1).rarity(Rarity.RARE), "whiteout")
    );
    public static final DeferredHolder<Item, PaintJobCanItem> GOLDEN_SPRAY_CAN = REGISTER.register(
            "golden_spray_can",
            () -> new PaintJobCanItem(baseProperties(Reference.id("golden_spray_can")).stacksTo(1).rarity(Rarity.EPIC), "golden")
    );
    public static final DeferredHolder<Item, KillEffectItem> CREEPER_BIRTHDAY_PARTY_BADGE = REGISTER.register(
            "creeper_birthday_party_badge",
            () -> new KillEffectItem(baseProperties(Reference.id("creeper_birthday_party_badge")).stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final DeferredHolder<Item, KillEffectItem> HEADPOPPER_BADGE = REGISTER.register(
            "headpoppper_badge",
            () -> new KillEffectItem(baseProperties(Reference.id("headpoppper_badge")).stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final DeferredHolder<Item, KillEffectItem> TRICKSHOT_BADGE = REGISTER.register(
            "trickshot_badge",
            () -> new KillEffectItem(baseProperties(Reference.id("trickshot_badge")).stacksTo(1).rarity(Rarity.EPIC))
    );
    public static final Map<ResourceLocation, DeferredHolder<Item, GunItem>> GUNS = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_HELMETS = new LinkedHashMap<>();
    public static final Map<BulletproofArmorItem.Tier, DeferredHolder<Item, BulletproofArmorItem>> BULLETPROOF_VESTS = new LinkedHashMap<>();
    // public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES = new LinkedHashMap<>();
    // public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_DIAMOND = new LinkedHashMap<>();
    // public static final Map<DyeColor, DeferredHolder<Item, ArmoredJoyHarnessItem>> ARMORED_JOY_HARNESSES_NETHERITE = new LinkedHashMap<>();
    private static final List<ResourceKey<Recipe<?>>> MANUAL_RECIPES;
    private static final ResourceLocation PHANTOM_SMG_ID = Reference.id("phantom_smg");
    private static final Set<ResourceLocation> DISABLED_GUN_IDS = Set.of(
            Reference.id("typhoonee")
    );

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
        registerBulletproofArmorItems();
        // registerArmoredHarnessItems(); // Disabled - armor system not available in 1.21.1
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
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.SKELETON_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_skeleton_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIE_SPAWN_EGG = REGISTER.register(
            "gunner_zombie_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIE,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.ZOMBIE_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_zombie_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_GHOUL_SPAWN_EGG = REGISTER.register(
            "gunner_ghoul_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    ModEntities.GHOUL.get(),
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.ZOMBIE_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_ghoul_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG = REGISTER.register(
            "gunner_zombified_piglin_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIFIED_PIGLIN,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_zombified_piglin_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PIGLIN_SPAWN_EGG = REGISTER.register(
            "gunner_piglin_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PIGLIN,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.PIGLIN_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_piglin_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_HUSK_SPAWN_EGG = REGISTER.register(
            "gunner_husk_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.HUSK,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.HUSK_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_husk_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_WITHER_SKELETON_SPAWN_EGG = REGISTER.register(
            "gunner_wither_skeleton_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.WITHER_SKELETON,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.WITHER_SKELETON_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_wither_skeleton_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_DROWNED_SPAWN_EGG = REGISTER.register(
            "gunner_drowned_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.DROWNED,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.DROWNED_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_drowned_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_ZOMBIE_VILLAGER_SPAWN_EGG = REGISTER.register(
            "gunner_zombie_villager_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.ZOMBIE_VILLAGER,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.ZOMBIE_VILLAGER_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_zombie_villager_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_STRAY_SPAWN_EGG = REGISTER.register(
            "gunner_stray_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.STRAY,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.STRAY_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_stray_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PILLAGER_SPAWN_EGG = REGISTER.register(
            "gunner_pillager_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PILLAGER,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.PILLAGER_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_pillager_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_VINDICATOR_SPAWN_EGG = REGISTER.register(
            "gunner_vindicator_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.VINDICATOR,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.VINDICATOR_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_vindicator_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, GunnerSpawnEggItem> GUNNER_PIGLIN_BRUTE_SPAWN_EGG = REGISTER.register(
            "gunner_piglin_brute_spawn_egg",
            () -> new GunnerSpawnEggItem(
                    EntityType.PIGLIN_BRUTE,
                    ModEntities.GHOUL.get(),
                    vanillaSpawnEgg(Items.PIGLIN_BRUTE_SPAWN_EGG),
                    baseProperties(Reference.id("gunner_piglin_brute_spawn_egg")).stacksTo(64)
            )
    );

    public static final DeferredHolder<Item, EnemyVehicleSpawnItem> ENEMY_LAV150_SPAWN_EGG = REGISTER.register(
            "enemy_lav150_spawn_egg",
            () -> new EnemyVehicleSpawnItem(ModEntities.LAV150::get, Reference.id("lav150"), baseProperties(Reference.id("enemy_lav150_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, EnemyVehicleSpawnItem> ENEMY_BMP2_SPAWN_EGG = REGISTER.register(
            "enemy_bmp2_spawn_egg",
            () -> new EnemyVehicleSpawnItem(ModEntities.BMP2::get, Reference.id("bmp2"), baseProperties(Reference.id("enemy_bmp2_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, EnemyVehicleSpawnItem> ENEMY_AH6_SPAWN_EGG = REGISTER.register(
            "enemy_ah6_spawn_egg",
            () -> new EnemyVehicleSpawnItem(ModEntities.AH6::get, Reference.id("ah6"), baseProperties(Reference.id("enemy_ah6_spawn_egg")).stacksTo(64))
    );

    public static final DeferredHolder<Item, EnemyVehicleSpawnItem> ENEMY_MI28_SPAWN_EGG = REGISTER.register(
            "enemy_mi28_spawn_egg",
            () -> new EnemyVehicleSpawnItem(ModEntities.MI28::get, Reference.id("mi28"), baseProperties(Reference.id("enemy_mi28_spawn_egg")).stacksTo(64))
    );

    private static SpawnEggItem vanillaSpawnEgg(Item item) {
        return (SpawnEggItem) item;
    }

    private static void registerAmmoItems() {
        for (String path : AMMO_IDS) {
            ResourceLocation id = Reference.id(path);
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
        for (Map.Entry<ResourceLocation, GunStats> entry : GunDefinitions.ALL.entrySet()) {
            ResourceLocation id = entry.getKey();
            GunStats stats = entry.getValue();
            DeferredHolder<Item, GunItem> holder = REGISTER.register(id.getPath(), () -> new AnimatedGunItem(defaultGunProperties(id, stats), stats));
            GUNS.put(id, holder);
        }
    }

    /* DISABLED: Armored harness items (Equipment API not available in 1.21.1)


    private static void registerArmoredHarnessItems() {


        // Method disabled


    }


    */

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
            if ("water_bomb".equals(ammo)) {
                continue;
            }
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id(ammo)));
        }
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("enhanced_coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("missile_engine")));
        addMagazineRecipes(keys);
        ATTACHMENTS.keySet().forEach(id -> keys.add(ResourceKey.create(Registries.RECIPE, id)));
        addCosmeticAttachmentRecipes(keys);
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_assembling_table")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_charging_station")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("magazine_loader")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("crowbar")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_kit")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_tool")));
        for (ResourceLocation id : GunDefinitions.ALL.keySet()) {
            if (!id.equals(PHANTOM_SMG_ID) && !isDisabledGunId(id)) {
                keys.add(ResourceKey.create(Registries.RECIPE, id));
            }
        }

        // Add bulletproof armor recipes
        for (BulletproofArmorItem.Tier tier : BulletproofArmorItem.Tier.values()) {
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("bulletproof_helmet_" + tier.suffix())));
            keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("bulletproof_vest_" + tier.suffix())));
        }

        return List.copyOf(keys);
    }

    private static void addCosmeticAttachmentRecipes(java.util.List<ResourceKey<Recipe<?>>> keys) {
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("classic_spray_can")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("toy_spray_can")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("whiteout_spray_can")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("golden_spray_can")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("creeper_birthday_party_badge")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("headpoppper_badge")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("trickshot_badge")));
    }

    public static List<ResourceKey<Recipe<?>>> manualRecipes() {
        return MANUAL_RECIPES;
    }

    public static List<ResourceKey<Recipe<?>>> unlockGunRecipeKeys() {
        java.util.ArrayList<ResourceKey<Recipe<?>>> keys = new java.util.ArrayList<>();
        for (ResourceLocation id : GunDefinitions.ALL.keySet()) {
            if (isDisabledGunId(id)) {
                continue;
            }
            keys.add(ResourceKey.create(Registries.RECIPE, id));
        }
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("enhanced_coolant")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("missile_engine")));
        addMagazineRecipes(keys);
        ATTACHMENTS.keySet().forEach(id -> keys.add(ResourceKey.create(Registries.RECIPE, id)));
        addCosmeticAttachmentRecipes(keys);
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_assembling_table")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("vehicle_charging_station")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("magazine_loader")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("crowbar")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_kit")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("repair_tool")));
        return List.copyOf(keys);
    }

    private static void addMagazineRecipes(java.util.List<ResourceKey<Recipe<?>>> keys) {
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("pistol_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("smg_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("smg_extended_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("smg_drum_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("rifle_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("rifle_extended_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("rifle_drum_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("shotgun_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("shotgun_extended_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("shotgun_drum_magazine")));
        keys.add(ResourceKey.create(Registries.RECIPE, Reference.id("machine_gun_magazine")));
    }

    public static boolean isDisabledGunId(ResourceLocation id) {
        return DISABLED_GUN_IDS.contains(id);
    }

    private static Item.Properties defaultGunProperties(ResourceLocation id, GunStats stats) {
        int baseDurability = switch (stats.reloadType()) {
            case "jeg:inventory_fed" -> 1024;
            case "jeg:manual" -> 256;
            default -> 512;
        };
        int durability = Math.max(1, (int) Math.round(baseDurability * 3.75D));
        // Note: .repairable() doesn't exist in 1.21.1 Item.Properties API
        // Repair logic should be handled via isValidRepairItem() override in GunItem
        return baseProperties(id).stacksTo(1).durability(durability);
    }

    private static Item.Properties baseProperties(ResourceLocation id) {
        // Note: .setId() doesn't exist in 1.21.1 - ID is set during registration
        return new Item.Properties();
    }

    private static DeferredHolder<Item, AttachmentItem> registerAttachment(
            String path,
            AttachmentType type,
            AttachmentModifiers modifiers,
            Item.Properties properties
    ) {
        return registerAttachment(path, () -> new AttachmentItem(type, modifiers, properties));
    }

    private static DeferredHolder<Item, AttachmentItem> registerAttachment(
            String path,
            Supplier<? extends AttachmentItem> supplier
    ) {
        ResourceLocation id = Reference.id(path);
        DeferredHolder<Item, AttachmentItem> holder = REGISTER.register(path, supplier);
        ATTACHMENTS.put(id, holder);
        return holder;
    }

    public static void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            GUNS.forEach((id, holder) -> {
                if (!id.equals(PHANTOM_SMG_ID) && !isDisabledGunId(id)) {
                    event.accept(holder.get());
                }
            });
            AMMO.values().forEach(holder -> event.accept(holder.get()));
            event.accept(MISSILE_ENGINE.get());
            event.accept(PISTOL_MAGAZINE.get());
            event.accept(SMG_MAGAZINE.get());
            event.accept(SMG_EXTENDED_MAGAZINE.get());
            event.accept(SMG_DRUM_MAGAZINE.get());
            event.accept(RIFLE_MAGAZINE.get());
            event.accept(RIFLE_EXTENDED_MAGAZINE.get());
            event.accept(RIFLE_DRUM_MAGAZINE.get());
            event.accept(SHOTGUN_MAGAZINE.get());
            event.accept(SHOTGUN_EXTENDED_MAGAZINE.get());
            event.accept(SHOTGUN_DRUM_MAGAZINE.get());
            event.accept(MACHINE_GUN_MAGAZINE.get());
            ATTACHMENTS.values().forEach(holder -> event.accept(holder.get()));
            event.accept(CLASSIC_SPRAY_CAN.get());
            event.accept(TOY_SPRAY_CAN.get());
            event.accept(WHITEOUT_SPRAY_CAN.get());
            event.accept(GOLDEN_SPRAY_CAN.get());
            event.accept(CREEPER_BIRTHDAY_PARTY_BADGE.get());
            event.accept(HEADPOPPER_BADGE.get());
            event.accept(TRICKSHOT_BADGE.get());
            BULLETPROOF_HELMETS.values().forEach(holder -> event.accept(holder.get()));
            BULLETPROOF_VESTS.values().forEach(holder -> event.accept(holder.get()));
            event.accept(PHANTOM_GUNNER_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            event.accept(GUNNER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_SPAWN_EGG.get());
            event.accept(GUNNER_GHOUL_SPAWN_EGG.get());
            event.accept(GUNNER_HUSK_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIFIED_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_PIGLIN_SPAWN_EGG.get());
            event.accept(GUNNER_WITHER_SKELETON_SPAWN_EGG.get());
            event.accept(ENEMY_LAV150_SPAWN_EGG.get());
            event.accept(ENEMY_BMP2_SPAWN_EGG.get());
            event.accept(ENEMY_AH6_SPAWN_EGG.get());
            event.accept(ENEMY_MI28_SPAWN_EGG.get());
        }

        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(GUNSMITH_MANUAL.get());
            event.accept(VEHICLE_ASSEMBLING_TABLE.get());
            event.accept(VEHICLE_CHARGING_STATION.get());
            event.accept(MAGAZINE_LOADER.get());
            event.accept(VEHICLE_CONTAINER.get());
            VehicleDataManager.all().keySet().stream()
                    .sorted()
                    .map(VehicleContainerBlockEntity::createItemForVehicle)
                    .forEach(event::accept);
            event.accept(CROWBAR.get());
            event.accept(REPAIR_KIT.get());
            event.accept(REPAIR_TOOL.get());
            event.accept(COOLANT.get());
            event.accept(ENHANCED_COOLANT.get());
    // ARMORED_JOY_HARNESSES.values().forEach(holder -> event.accept(holder.get()));
    // ARMORED_JOY_HARNESSES_DIAMOND.values().forEach(holder -> event.accept(holder.get()));
    // ARMORED_JOY_HARNESSES_NETHERITE.values().forEach(holder -> event.accept(holder.get()));
        }

        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(PHANTOM_GUNNER_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_SPAWN_EGG.get());
            event.accept(TERROR_PHANTOM_GUARDIAN_SPAWN_EGG.get());
            event.accept(GUNNER_SKELETON_SPAWN_EGG.get());
            event.accept(GUNNER_ZOMBIE_SPAWN_EGG.get());
            event.accept(GUNNER_GHOUL_SPAWN_EGG.get());
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
            event.accept(ENEMY_LAV150_SPAWN_EGG.get());
            event.accept(ENEMY_BMP2_SPAWN_EGG.get());
            event.accept(ENEMY_AH6_SPAWN_EGG.get());
            event.accept(ENEMY_MI28_SPAWN_EGG.get());
        }
    }

}
