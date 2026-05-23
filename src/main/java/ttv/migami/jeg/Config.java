package ttv.migami.jeg;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue LEGACY_BULLET_TRAIL_ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_AMMO_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_TIMERS_HUD;
    public static final ModConfigSpec.ConfigValue<String> CROSSHAIR;
    public static final ModConfigSpec.BooleanValue SHOW_HITMARKER;
    public static final ModConfigSpec.ConfigValue<String> DYNAMIC_CROSSHAIR_DOT_MODE;
    public static final ModConfigSpec.DoubleValue DYNAMIC_CROSSHAIR_DOT_THRESHOLD;

    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_MAX_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_MAX_CHANCE;
    public static final ModConfigSpec.BooleanValue PHANTOM_GUNNER_DEATH_EXPLOSION_ENABLED;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_MAX_CHANCE;
    public static final ModConfigSpec.DoubleValue SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIE_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue HUSK_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PARCHED_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIFIED_PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue WITHER_SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.IntValue SPAWN_SCALING_START_DAY;
    public static final ModConfigSpec.IntValue SPAWN_SCALING_DAYS_TO_MAX;
    public static final ModConfigSpec.BooleanValue RECOIL_BACKSTEP_ENABLED;
    public static final ModConfigSpec.DoubleValue RECOIL_BACKSTEP_SCALE;
    public static final ModConfigSpec.BooleanValue BLOCK_HIT_ANIMATION_ENABLED;
    public static final ModConfigSpec.BooleanValue BULLET_BLOCK_DESTRUCTION_ENABLED;
    public static final ModConfigSpec.BooleanValue MAGAZINE_FEED_ENABLED;
    public static final ModConfigSpec.BooleanValue GUNNER_TERRAIN_PLACEMENT_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> GUNNER_TERRAIN_SUPPORT_BLOCK;
    public static final ModConfigSpec.IntValue GUNNER_TERRAIN_BREAK_MAX_TIER;
    public static final ModConfigSpec.IntValue GUNNER_ACCURACY_START_DAY;
    public static final ModConfigSpec.IntValue GUNNER_ACCURACY_MAX_DAY;
    public static final ModConfigSpec.DoubleValue GUNNER_ACCURACY_MAX_PERCENT;
    public static final ModConfigSpec.DoubleValue GUNNER_SHOTGUN_SPREAD_MULTIPLIER;
    public static final ModConfigSpec.IntValue GUNNER_PROGRESSION_MAX_DAY;
    public static final ModConfigSpec.IntValue TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_RESET_TICKS;
    public static final ModConfigSpec.IntValue TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_WARMUP_HITS;
    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_MINIGUN_RAPID_FIRE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_LIGHT_MACHINE_GUN_RAPID_FIRE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TERROR_RAID_WAVE_INTERVAL_SECONDS;
    public static final ModConfigSpec.IntValue TERROR_RAID_GROUND_WAVE_COUNT;
    public static final ModConfigSpec.IntValue TERROR_RAID_AIR_WAVE_COUNT;
    public static final ModConfigSpec.BooleanValue FACTION_PATROL_ENABLED;
    public static final ModConfigSpec.IntValue FACTION_PATROL_INTERVAL_DAYS;
    public static final ModConfigSpec.IntValue FACTION_PATROL_RANDOM_INTERVAL_MIN_TICKS;
    public static final ModConfigSpec.IntValue FACTION_PATROL_RANDOM_INTERVAL_MAX_TICKS;
    public static final ModConfigSpec.IntValue FACTION_PATROL_MINIMUM_DAYS;
    public static final ModConfigSpec.IntValue FACTION_PATROL_BOSSBAR_RANGE;
    public static final ModConfigSpec.DoubleValue FACTION_PATROL_SPAWN_CHANCE;
    public static final ModConfigSpec.BooleanValue FACTION_RAID_ENABLED;
    public static final ModConfigSpec.IntValue FACTION_RAID_INTERVAL_DAYS;
    public static final ModConfigSpec.IntValue FACTION_RAID_RANDOM_INTERVAL_MIN_TICKS;
    public static final ModConfigSpec.IntValue FACTION_RAID_RANDOM_INTERVAL_MAX_TICKS;
    public static final ModConfigSpec.IntValue FACTION_RAID_MINIMUM_DAYS;
    public static final ModConfigSpec.IntValue FACTION_RAID_HOME_TRIGGER_RADIUS;
    public static final ModConfigSpec.BooleanValue VEHICLE_ENABLED;
    public static final ModConfigSpec.IntValue BULLET_LIFETIME_SECONDS;
    public static final ModConfigSpec.BooleanValue UI_SHOW_CROSSHAIR;
    public static final ModConfigSpec.BooleanValue UI_SHOW_HIT_FEEDBACK;
    private static final Map<String, ModConfigSpec.ConfigValue<?>> COMMAND_CONFIGS = new LinkedHashMap<>();
    private static final Map<String, Map<String, ModConfigSpec.DoubleValue>> GUNNER_GROWTH_CONFIGS = new LinkedHashMap<>();
    private static final String DEFAULT_GUNNER_TERRAIN_SUPPORT_BLOCK = "minecraft:dirt";
    private static final String[] GUNNER_GROWTH_TYPES = {
            "all", "skeleton", "stray", "zombie", "husk", "parched", "drowned", "zombieVillager",
            "zombifiedPiglin", "piglin", "piglinBrute", "witherSkeleton", "pillager", "vindicator", "generic"
    };
    private static final String[] GUNNER_GROWTH_SETTINGS = {
            "minSpawnChance", "maxSpawnChance", "spawnChancePerDay",
            "weaponInitialTier", "weaponMaxTier", "weaponTierPerDay",
            "armorInitialTier", "armorMaxTier", "armorTierPerDay",
            "rocketLauncherStartDay", "rocketLauncherChance", "weaponAggression"
    };

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("rendering");
        LEGACY_BULLET_TRAIL_ENABLED = clientBuilder
                .comment("If true, use legacy 1.20.1-style bullet trail rendering.")
                .define("legacyBulletTrailEnabled", true);
        SHOW_AMMO_HUD = clientBuilder
                .comment("If true, render the weapon ammo HUD in the lower-right corner.")
                .define("showAmmoHud", true);
        SHOW_TIMERS_HUD = clientBuilder
                .comment("If true, render timer bars such as overheat and water cooling.")
                .define("showTimersHud", true);
        CROSSHAIR = clientBuilder
                .comment("Custom gun crosshair id. Use default, jeg:dynamic, jeg:tech, or a texture id under textures/crosshair.")
                .define("crosshair", "jeg:dynamic");
        SHOW_HITMARKER = clientBuilder
                .comment("If true, render a short marker when a bullet hits a living entity.")
                .define("showHitmarker", true);
        DYNAMIC_CROSSHAIR_DOT_MODE = clientBuilder
                .comment("Dynamic crosshair dot mode: never, at_min_spread, threshold, always.")
                .define("dynamicCrosshairDotMode", "at_min_spread");
        DYNAMIC_CROSSHAIR_DOT_THRESHOLD = clientBuilder
                .comment("Spread threshold where the dynamic crosshair center dot is shown.")
                .defineInRange("dynamicCrosshairDotThreshold", 0.8D, 0.0D, 90.0D);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();

        serverBuilder.push("ui");
        UI_SHOW_CROSSHAIR = serverBuilder
                .comment("If true, clients render the JEG gun crosshair while holding guns.")
                .define("showCrosshair", true);
        UI_SHOW_HIT_FEEDBACK = serverBuilder
                .comment("If true, clients display hit feedback markers when bullets hit living entities.")
                .define("showHitFeedback", true);
        serverBuilder.pop();

        serverBuilder.push("spawns");
        TERROR_PHANTOM_NATURAL_CHANCE = serverBuilder
                .comment("Base probability (0-1) that a naturally spawned Phantom upgrades into a Terror Phantom at early game.")
                .defineInRange("terrorPhantomChance", 0.01D, 0.0D, 1.0D);

        TERROR_PHANTOM_MAX_CHANCE = serverBuilder
                .comment("Upper cap probability (0-1) for natural Terror Phantom conversion as game time increases.")
                .defineInRange("terrorPhantomMaxChance", 0.07D, 0.0D, 1.0D);

        PHANTOM_GUNNER_NATURAL_CHANCE = serverBuilder
                .comment("Base probability (0-1) that a naturally spawned Phantom upgrades into a Phantom Gunner (when not converted to Terror Phantom).")
                .defineInRange("phantomGunnerChance", 0.12D, 0.0D, 1.0D);

        PHANTOM_GUNNER_MAX_CHANCE = serverBuilder
                .comment("Upper cap probability (0-1) for Phantom Gunner conversion as game time increases.")
                .defineInRange("phantomGunnerMaxChance", 0.33D, 0.0D, 1.0D);

        PHANTOM_GUNNER_DEATH_EXPLOSION_ENABLED = serverBuilder
                .comment("If true, Phantom Gunners explode when they die.")
                .define("phantomGunnerDeathExplosionEnabled", true);

        PILLAGER_GUNNER_CHANCE = serverBuilder
                .comment("Base probability (0-1) that a naturally spawned Pillager converts into a Pillager Gunner.")
                .defineInRange("pillagerGunnerChance", 0.14D, 0.0D, 1.0D);

        PILLAGER_GUNNER_MAX_CHANCE = serverBuilder
                .comment("Upper cap probability (0-1) for Pillager Gunner conversion as game time increases.")
                .defineInRange("pillagerGunnerMaxChance", 0.35D, 0.0D, 1.0D);

        SKELETON_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Skeleton is flagged to receive a gun on spawn completion.")
                .defineInRange("skeletonGunnerChance", 1.0D / 7.0D, 0.0D, 1.0D);

        ZOMBIE_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Zombie converts into a Zombie Gunner. Should be lower than Skeleton Gunner chance.")
                .defineInRange("zombieGunnerChance", 1.0D / 12.0D, 0.0D, 1.0D);

        HUSK_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Husk converts into a Husk Gunner. Same as Zombie Gunner chance.")
                .defineInRange("huskGunnerChance", 1.0D / 12.0D, 0.0D, 1.0D);

        PARCHED_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Parched converts into a Parched Gunner. Defaults to the Husk Gunner chance.")
                .defineInRange("parchedGunnerChance", 1.0D / 12.0D, 0.0D, 1.0D);

        ZOMBIFIED_PIGLIN_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Zombified Piglin converts into a Zombified Piglin Gunner. Should be lower than Skeleton Gunner chance.")
                .defineInRange("zombifiedPiglinGunnerChance", 1.0D / 10.0D, 0.0D, 1.0D);

        PIGLIN_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Piglin converts into a Piglin Gunner. Should be higher than Pillager Gunner chance.")
                .defineInRange("piglinGunnerChance", 0.30D, 0.0D, 1.0D);

        WITHER_SKELETON_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Wither Skeleton converts into a Wither Skeleton Gunner. Wither Skeletons prefer heavy weapons.")
                .defineInRange("witherSkeletonGunnerChance", 0.25D, 0.0D, 1.0D);

        SPAWN_SCALING_START_DAY = serverBuilder
                .comment("In-game day when dynamic spawn scaling starts.")
                .defineInRange("spawnScalingStartDay", 3, 0, 5000);

        SPAWN_SCALING_DAYS_TO_MAX = serverBuilder
                .comment("Number of in-game days from scaling start day to reach max spawn probabilities.")
                .defineInRange("spawnScalingDaysToMax", 28, 1, 5000);
        serverBuilder.pop();
        defineGunnerGrowthConfig(serverBuilder);

        serverBuilder.push("combat");
        RECOIL_BACKSTEP_ENABLED = serverBuilder
                .comment("If true, firing a gun pushes the player backwards.")
                .define("recoilBackstepEnabled", true);
        RECOIL_BACKSTEP_SCALE = serverBuilder
                .comment("Global multiplier for recoil backstep force. 0.5 means half of 1.20.1-like force.")
                .defineInRange("recoilBackstepScale", 0.5D, 0.0D, 2.0D);
        BLOCK_HIT_ANIMATION_ENABLED = serverBuilder
                .comment("If true, bullets hitting a block trigger the vanilla block hit particle animation event.")
                .define("blockHitAnimationEnabled", true);
        BULLET_BLOCK_DESTRUCTION_ENABLED = serverBuilder
                .comment("If true, bullets can destroy hit blocks based on penetration and bullet power rules.")
                .define("bulletBlockDestructionEnabled", true);
        MAGAZINE_FEED_ENABLED = serverBuilder
                .comment("If true, guns that support magazine swaps will reload from compatible magazines instead of using the legacy loose-ammo reload path.")
                .define("magazineFeedEnabled", true);
        GUNNER_TERRAIN_PLACEMENT_ENABLED = serverBuilder
                .comment("If true, ground gunners can place support blocks to get around simple terrain obstacles.")
                .define("gunnerTerrainPlacementEnabled", true);
        GUNNER_TERRAIN_SUPPORT_BLOCK = serverBuilder
                .comment("Block id ground gunners place as temporary terrain support.")
                .define("gunnerTerrainSupportBlock", DEFAULT_GUNNER_TERRAIN_SUPPORT_BLOCK);
        GUNNER_TERRAIN_BREAK_MAX_TIER = serverBuilder
                .comment("Maximum bulletproof tier ground gunners are allowed to break while traversing terrain. 0 = penetrable only, 2 = tier 2 and below.")
                .defineInRange("gunnerTerrainBreakMaxTier", 2, 0, 3);
        GUNNER_ACCURACY_START_DAY = serverBuilder
                .comment("In-game day when gunner accuracy scaling starts.")
                .defineInRange("gunnerAccuracyStartDay", 5, 0, 5000);
        GUNNER_ACCURACY_MAX_DAY = serverBuilder
                .comment("In-game day when gunner accuracy reaches configured max value.")
                .defineInRange("gunnerAccuracyMaxDay", 60, 1, 5000);
        GUNNER_ACCURACY_MAX_PERCENT = serverBuilder
                .comment("Late-game gunner accuracy increase as a percent from the day-5 spread baseline. 0.70 means 70% more accurate.")
                .defineInRange("gunnerAccuracyMaxPercent", 0.70D, 0.0D, 0.95D);
        GUNNER_SHOTGUN_SPREAD_MULTIPLIER = serverBuilder
                .comment("Additional spread multiplier for gunner-fired shotguns. Lower = tighter pellet grouping.")
                .defineInRange("gunnerShotgunSpreadMultiplier", 0.82D, 0.2D, 1.0D);
        GUNNER_PROGRESSION_MAX_DAY = serverBuilder
                .comment("In-game day when gunner weapon strength, armor chance, and armor tier reach their maximum.")
                .defineInRange("gunnerProgressionMaxDay", 60, 1, 5000);
        TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_RESET_TICKS = serverBuilder
                .comment("Ticks without qualifying minigun/light machine gun hits before Terror Phantom rapid-fire resistance resets.")
                .defineInRange("terrorPhantomRapidFireResistanceResetTicks", 25, 1, 200);
        TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_WARMUP_HITS = serverBuilder
                .comment("Qualifying hits at the start of each burst before Terror Phantom rapid-fire resistance reduces damage.")
                .defineInRange("terrorPhantomRapidFireResistanceWarmupHits", 10, 0, 200);
        TERROR_PHANTOM_MINIGUN_RAPID_FIRE_DAMAGE_MULTIPLIER = serverBuilder
                .comment("Extra damage multiplier for player minigun hits after Terror Phantom rapid-fire resistance warms up. 1.0 disables this extra resistance.")
                .defineInRange("terrorPhantomMinigunRapidFireDamageMultiplier", 0.18D, 0.01D, 1.0D);
        TERROR_PHANTOM_LIGHT_MACHINE_GUN_RAPID_FIRE_DAMAGE_MULTIPLIER = serverBuilder
                .comment("Extra damage multiplier for player light machine gun hits after Terror Phantom rapid-fire resistance warms up. 1.0 disables this extra resistance.")
                .defineInRange("terrorPhantomLightMachineGunRapidFireDamageMultiplier", 0.35D, 0.01D, 1.0D);
        serverBuilder.pop();

        serverBuilder.push("terrorRaid");
        TERROR_RAID_WAVE_INTERVAL_SECONDS = serverBuilder
                .comment("Seconds between Terror Raid waves.")
                .defineInRange("waveIntervalSeconds", 18, 3, 120);
        TERROR_RAID_GROUND_WAVE_COUNT = serverBuilder
                .comment("Total ground raid waves triggered by Terror Phantom death.")
                .defineInRange("groundWaveCount", 4, 1, 10);
        TERROR_RAID_AIR_WAVE_COUNT = serverBuilder
                .comment("Total air raid waves triggered by Bound Terror Phantom death.")
                .defineInRange("airWaveCount", 4, 1, 10);
        serverBuilder.pop();

        serverBuilder.push("factionPatrol");
        FACTION_PATROL_ENABLED = serverBuilder
                .comment("If true, faction patrols spawn naturally and command-spawned patrols show encounter boss bars.")
                .define("enabled", true);
        FACTION_PATROL_INTERVAL_DAYS = serverBuilder
                .comment("Fixed patrol interval in days. Set to 0 to use random interval ticks.")
                .defineInRange("intervalDays", 5, 0, 30);
        FACTION_PATROL_RANDOM_INTERVAL_MIN_TICKS = serverBuilder
                .comment("Minimum random patrol interval in ticks when intervalDays is 0.")
                .defineInRange("randomIntervalMinTicks", 12000, 1, Integer.MAX_VALUE);
        FACTION_PATROL_RANDOM_INTERVAL_MAX_TICKS = serverBuilder
                .comment("Maximum random patrol interval in ticks when intervalDays is 0.")
                .defineInRange("randomIntervalMaxTicks", 24000, 1, Integer.MAX_VALUE);
        FACTION_PATROL_MINIMUM_DAYS = serverBuilder
                .comment("Minimum in-game days before patrols can naturally spawn.")
                .defineInRange("minimumDays", 5, 0, 100);
        FACTION_PATROL_BOSSBAR_RANGE = serverBuilder
                .comment("Range in blocks where patrol boss bars are visible.")
                .defineInRange("bossBarRange", 64, 16, 256);
        FACTION_PATROL_SPAWN_CHANCE = serverBuilder
                .comment("Probability (0-1) that a patrol attempt actually spawns once all other patrol conditions pass.")
                .defineInRange("spawnChance", 0.35D, 0.0D, 1.0D);
        serverBuilder.pop();

        serverBuilder.push("factionRaid");
        FACTION_RAID_ENABLED = serverBuilder
                .comment("If true, faction raids can spawn naturally and by returning home with Faction Omen.")
                .define("enabled", true);
        FACTION_RAID_INTERVAL_DAYS = serverBuilder
                .comment("Fixed raid interval in days. Set to 0 to use random interval ticks.")
                .defineInRange("intervalDays", 30, 0, 100);
        FACTION_RAID_RANDOM_INTERVAL_MIN_TICKS = serverBuilder
                .comment("Minimum random raid interval in ticks when intervalDays is 0.")
                .defineInRange("randomIntervalMinTicks", 12000, 1, Integer.MAX_VALUE);
        FACTION_RAID_RANDOM_INTERVAL_MAX_TICKS = serverBuilder
                .comment("Maximum random raid interval in ticks when intervalDays is 0.")
                .defineInRange("randomIntervalMaxTicks", 24000, 1, Integer.MAX_VALUE);
        FACTION_RAID_MINIMUM_DAYS = serverBuilder
                .comment("Minimum in-game days before natural raids can spawn.")
                .defineInRange("minimumDays", 15, 0, 100);
        FACTION_RAID_HOME_TRIGGER_RADIUS = serverBuilder
                .comment("Radius around player's respawn point that triggers a faction raid when Faction Omen is active.")
                .defineInRange("homeTriggerRadius", 48, 8, 256);
        serverBuilder.pop();

        serverBuilder.push("vehicle");
        VEHICLE_ENABLED = serverBuilder
                .comment("If true, players can place vehicle assembling tables and assemble new vehicle containers.")
                .define("enabled", true);
        serverBuilder.pop();

        serverBuilder.push("combat");
        BULLET_LIFETIME_SECONDS = serverBuilder
                .comment("Global bullet lifetime in seconds. Applies to all bullets.")
                .defineInRange("bulletLifetimeSeconds", 60, 1, 3600);
        serverBuilder.pop();

        SERVER_SPEC = serverBuilder.build();

        registerCommandConfig("ui.showCrosshair", UI_SHOW_CROSSHAIR);
        registerCommandConfig("ui.showHitFeedback", UI_SHOW_HIT_FEEDBACK);
        registerCommandConfig("patrol.enabled", FACTION_PATROL_ENABLED);
        registerCommandConfig("patrol.intervalDays", FACTION_PATROL_INTERVAL_DAYS);
        registerCommandConfig("patrol.minimumDays", FACTION_PATROL_MINIMUM_DAYS);
        registerCommandConfig("patrol.spawnChance", FACTION_PATROL_SPAWN_CHANCE);
        registerCommandConfig("mob.mechanism.terrorPhantom.chance", TERROR_PHANTOM_NATURAL_CHANCE);
        registerCommandConfig("mob.mechanism.terrorPhantom.maxChance", TERROR_PHANTOM_MAX_CHANCE);
        registerCommandConfig("mob.mechanism.phantomGunner.deathExplosion", PHANTOM_GUNNER_DEATH_EXPLOSION_ENABLED);
        registerCommandConfig("combat.bulletBlockDestruction", BULLET_BLOCK_DESTRUCTION_ENABLED);
        registerCommandConfig("combat.magazineFeed", MAGAZINE_FEED_ENABLED);
        registerCommandConfig("combat.gunnerTerrainPlacement.enabled", GUNNER_TERRAIN_PLACEMENT_ENABLED);
        registerCommandConfig("combat.gunnerTerrainPlacement.block", GUNNER_TERRAIN_SUPPORT_BLOCK);
        registerCommandConfig("combat.gunnerTerrainBreak.maxTier", GUNNER_TERRAIN_BREAK_MAX_TIER);
        registerGunnerGrowthCommandConfigs();
        registerCommandConfig("vehicle.enabled", VEHICLE_ENABLED);
    }

    private static void defineGunnerGrowthConfig(ModConfigSpec.Builder serverBuilder) {
        serverBuilder.push("gunnerGrowth");
        for (String type : GUNNER_GROWTH_TYPES) {
            serverBuilder.push(type);
            Map<String, ModConfigSpec.DoubleValue> values = new LinkedHashMap<>();
            for (String setting : GUNNER_GROWTH_SETTINGS) {
                values.put(setting, serverBuilder
                        .comment("Use -1 to inherit the balanced default or the all-gunners override.")
                        .defineInRange(setting, defaultGunnerGrowthConfigValue(type, setting), -1.0D, 5000.0D));
            }
            GUNNER_GROWTH_CONFIGS.put(type, values);
            serverBuilder.pop();
        }
        serverBuilder.pop();
    }

    private static double defaultGunnerGrowthConfigValue(String type, String setting) {
        if (!"all".equals(type)) {
            return -1.0D;
        }
        return switch (setting) {
            case "weaponInitialTier" -> 0.0D;
            case "weaponMaxTier" -> 3.0D;
            case "weaponTierPerDay" -> 0.04D;
            case "armorInitialTier" -> 0.0D;
            case "armorMaxTier" -> 5.0D;
            case "armorTierPerDay" -> 0.05D;
            case "rocketLauncherStartDay" -> 80.0D;
            case "rocketLauncherChance" -> 0.015D;
            case "weaponAggression" -> 0.55D;
            default -> -1.0D;
        };
    }

    private static void registerGunnerGrowthCommandConfigs() {
        for (Map.Entry<String, Map<String, ModConfigSpec.DoubleValue>> typeEntry : GUNNER_GROWTH_CONFIGS.entrySet()) {
            for (Map.Entry<String, ModConfigSpec.DoubleValue> settingEntry : typeEntry.getValue().entrySet()) {
                registerCommandConfig(gunnerGrowthCommandKey(typeEntry.getKey(), settingEntry.getKey()), settingEntry.getValue());
            }
        }
    }

    private Config() {}

    public static void saveServerConfig() {
        SERVER_SPEC.save();
    }

    public static Object getConfigValue(String key) {
        ModConfigSpec.ConfigValue<?> value = COMMAND_CONFIGS.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown config key: " + key);
        }
        return value.get();
    }

    public static void setConfigValue(String key, Object rawValue) {
        ModConfigSpec.ConfigValue<?> value = COMMAND_CONFIGS.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown config key: " + key);
        }
        setValue(value, rawValue);
    }

    private static void registerCommandConfig(String key, ModConfigSpec.ConfigValue<?> value) {
        COMMAND_CONFIGS.put(key, value);
    }

    @SuppressWarnings("unchecked")
    private static void setValue(ModConfigSpec.ConfigValue<?> value, Object rawValue) {
        if (value instanceof ModConfigSpec.BooleanValue booleanValue) {
            booleanValue.set((Boolean) rawValue);
            return;
        }
        if (value instanceof ModConfigSpec.IntValue intValue) {
            intValue.set((Integer) rawValue);
            return;
        }
        if (value instanceof ModConfigSpec.DoubleValue doubleValue) {
            doubleValue.set((Double) rawValue);
            return;
        }
        if (rawValue instanceof String stringValue) {
            ((ModConfigSpec.ConfigValue<String>) value).set(stringValue);
            return;
        }
        throw new IllegalArgumentException("Unsupported config value type for " + String.join(".", value.getPath()));
    }

    public static double terrorPhantomChance() {
        return clamp01(TERROR_PHANTOM_NATURAL_CHANCE.get());
    }

    public static double terrorPhantomChance(Level level) {
        return scaledChance(level, terrorPhantomChance(), clamp01(TERROR_PHANTOM_MAX_CHANCE.get()));
    }

    public static double phantomGunnerChance() {
        return clamp01(PHANTOM_GUNNER_NATURAL_CHANCE.get());
    }

    public static double phantomGunnerChance(Level level) {
        return scaledChance(level, phantomGunnerChance(), clamp01(PHANTOM_GUNNER_MAX_CHANCE.get()));
    }

    public static boolean phantomGunnerDeathExplosionEnabled() {
        return PHANTOM_GUNNER_DEATH_EXPLOSION_ENABLED.get();
    }

    public static double pillagerGunnerChance() {
        return clamp01(PILLAGER_GUNNER_CHANCE.get());
    }

    public static double pillagerGunnerChance(Level level) {
        return scaledChance(level, pillagerGunnerChance(), clamp01(PILLAGER_GUNNER_MAX_CHANCE.get()));
    }

    public static double skeletonGunnerChance() {
        return clamp01(SKELETON_GUNNER_CHANCE.get());
    }

    public static double zombieGunnerChance() {
        return clamp01(ZOMBIE_GUNNER_CHANCE.get());
    }

    public static double huskGunnerChance() {
        return clamp01(HUSK_GUNNER_CHANCE.get());
    }

    public static double parchedGunnerChance() {
        return clamp01(PARCHED_GUNNER_CHANCE.get());
    }

    public static double zombifiedPiglinGunnerChance() {
        return clamp01(ZOMBIFIED_PIGLIN_GUNNER_CHANCE.get());
    }

    public static double piglinGunnerChance() {
        return clamp01(PIGLIN_GUNNER_CHANCE.get());
    }

    public static double witherSkeletonGunnerChance() {
        return clamp01(WITHER_SKELETON_GUNNER_CHANCE.get());
    }

    public static int terrorRaidWaveIntervalSeconds() {
        return Math.max(3, TERROR_RAID_WAVE_INTERVAL_SECONDS.get());
    }

    public static boolean recoilBackstepEnabled() {
        return RECOIL_BACKSTEP_ENABLED.get();
    }

    public static double recoilBackstepScale() {
        return Math.max(0.0D, RECOIL_BACKSTEP_SCALE.get());
    }

    public static boolean blockHitAnimationEnabled() {
        return BLOCK_HIT_ANIMATION_ENABLED.get();
    }

    public static boolean bulletBlockDestructionEnabled() {
        return BULLET_BLOCK_DESTRUCTION_ENABLED.get();
    }

    public static boolean magazineFeedEnabled() {
        return MAGAZINE_FEED_ENABLED.get();
    }

    public static void setBulletBlockDestructionEnabled(boolean enabled) {
        setConfigValue("combat.bulletBlockDestruction", enabled);
        saveServerConfig();
    }

    public static boolean gunnerTerrainPlacementEnabled() {
        return GUNNER_TERRAIN_PLACEMENT_ENABLED.get();
    }

    public static String gunnerTerrainSupportBlock() {
        return normalizeGunnerTerrainSupportBlockId(GUNNER_TERRAIN_SUPPORT_BLOCK.get(), false);
    }

    public static BlockState gunnerTerrainSupportBlockState() {
        return resolveGunnerTerrainSupportBlock().defaultBlockState();
    }

    public static String normalizeGunnerTerrainSupportBlockId(String rawValue) {
        return normalizeGunnerTerrainSupportBlockId(rawValue, true);
    }

    private static String normalizeGunnerTerrainSupportBlockId(String rawValue, boolean failOnInvalid) {
        Identifier id = parseBlockId(rawValue);
        Block block = id == null ? null : findBlock(id);
        if (block == null || block == Blocks.AIR) {
            if (failOnInvalid) {
                throw new IllegalArgumentException("Unknown or invalid block id: " + rawValue);
            }
            return DEFAULT_GUNNER_TERRAIN_SUPPORT_BLOCK;
        }
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static Block resolveGunnerTerrainSupportBlock() {
        Identifier id = parseBlockId(GUNNER_TERRAIN_SUPPORT_BLOCK.get());
        Block block = id == null ? null : findBlock(id);
        return block == null || block == Blocks.AIR ? Blocks.DIRT : block;
    }

    private static Identifier parseBlockId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String value = rawValue.contains(":") ? rawValue : "minecraft:" + rawValue;
        return Identifier.tryParse(value);
    }

    private static Block findBlock(Identifier id) {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
                return block;
            }
        }
        return null;
    }

    public static int gunnerTerrainBreakMaxTier() {
        return Mth.clamp(GUNNER_TERRAIN_BREAK_MAX_TIER.get(), 0, 3);
    }

    public static int gunnerAccuracyStartDay() {
        return Math.max(0, GUNNER_ACCURACY_START_DAY.get());
    }

    public static int gunnerAccuracyMaxDay() {
        return Math.max(1, GUNNER_ACCURACY_MAX_DAY.get());
    }

    public static double gunnerAccuracyMaxPercent() {
        return Mth.clamp(GUNNER_ACCURACY_MAX_PERCENT.get(), 0.0D, 0.95D);
    }

    public static double gunnerShotgunSpreadMultiplier() {
        return Mth.clamp(GUNNER_SHOTGUN_SPREAD_MULTIPLIER.get(), 0.2D, 1.0D);
    }

    public static int gunnerProgressionMaxDay() {
        return Math.max(1, GUNNER_PROGRESSION_MAX_DAY.get());
    }

    public static float gunnerProgressionScale(Level level) {
        long day = currentGunnerDay(level);
        return Mth.clamp((float) day / (float) gunnerProgressionMaxDay(), 0.0F, 1.0F);
    }

    public static String[] gunnerGrowthTypes() {
        return GUNNER_GROWTH_TYPES.clone();
    }

    public static String[] gunnerGrowthSettings() {
        return GUNNER_GROWTH_SETTINGS.clone();
    }

    public static String gunnerGrowthCommandKey(String type, String setting) {
        if (!contains(GUNNER_GROWTH_TYPES, type) || !contains(GUNNER_GROWTH_SETTINGS, setting)) {
            throw new IllegalArgumentException("Unknown gunner growth config: " + type + "." + setting);
        }
        return "mob.spawn." + type + "." + setting;
    }

    public static double gunnerSpawnChance(Level level, String gunnerType, double legacyBaseChance) {
        // Treat the old mob.*GunnerChance value as the default minimum, then grow toward the new cap.
        double min = resolveGunnerGrowthValue(gunnerType, "minSpawnChance", clamp01(legacyBaseChance));
        double max = resolveGunnerGrowthValue(gunnerType, "maxSpawnChance", defaultGunnerMaxSpawnChance(gunnerType, min));
        if (max < min) {
            max = min;
        }
        double defaultGrowth = Math.max(0.0D, (max - min) / 60.0D);
        double growth = resolveGunnerGrowthValue(gunnerType, "spawnChancePerDay", defaultGrowth);
        long day = Math.max(0L, currentGunnerDay(level) - Math.max(0, SPAWN_SCALING_START_DAY.get()));
        return clamp01(Math.min(max, min + day * Math.max(0.0D, growth)));
    }

    public static int gunnerWeaponMaxTier(Level level, String gunnerType) {
        return scaledTier(level, gunnerType, "weaponInitialTier", "weaponMaxTier", "weaponTierPerDay", 0, 3);
    }

    public static int gunnerArmorMaxTier(Level level, String gunnerType) {
        return scaledTier(level, gunnerType, "armorInitialTier", "armorMaxTier", "armorTierPerDay", 0, 6);
    }

    public static double gunnerWeaponAggression(String gunnerType) {
        return Mth.clamp(resolveGunnerGrowthValue(gunnerType, "weaponAggression", 0.55D), 0.0D, 1.0D);
    }

    public static boolean shouldGunnerUseRocketLauncher(Level level, String gunnerType, RandomSource random) {
        // Rocket launchers are gated outside the normal weapon-tier roll so their rate stays independently tunable.
        long startDay = Math.round(resolveGunnerGrowthValue(gunnerType, "rocketLauncherStartDay", 80.0D));
        if (currentGunnerDay(level) < startDay) {
            return false;
        }
        double chance = Mth.clamp(resolveGunnerGrowthValue(gunnerType, "rocketLauncherChance", 0.015D), 0.0D, 1.0D);
        return chance > 0.0D && random.nextDouble() < chance;
    }

    private static int scaledTier(Level level, String gunnerType, String initialKey, String maxKey, String growthKey, int min, int max) {
        double initial = resolveGunnerGrowthValue(gunnerType, initialKey, min);
        double cap = resolveGunnerGrowthValue(gunnerType, maxKey, max);
        double growth = resolveGunnerGrowthValue(gunnerType, growthKey, 0.0D);
        double scaled = initial + currentGunnerDay(level) * Math.max(0.0D, growth);
        return Mth.clamp((int) Math.floor(Math.min(cap, scaled)), min, max);
    }

    private static double resolveGunnerGrowthValue(String gunnerType, String setting, double fallback) {
        double typed = directGunnerGrowthValue(gunnerType, setting);
        if (typed >= 0.0D) {
            return typed;
        }
        double global = directGunnerGrowthValue("all", setting);
        return global >= 0.0D ? global : fallback;
    }

    private static double directGunnerGrowthValue(String gunnerType, String setting) {
        Map<String, ModConfigSpec.DoubleValue> values = GUNNER_GROWTH_CONFIGS.get(gunnerType);
        if (values == null) {
            values = GUNNER_GROWTH_CONFIGS.get("generic");
        }
        ModConfigSpec.DoubleValue value = values != null ? values.get(setting) : null;
        return value != null ? value.get() : -1.0D;
    }

    private static double defaultGunnerMaxSpawnChance(String gunnerType, double min) {
        double cap = switch (gunnerType) {
            case "skeleton", "stray" -> 0.32D;
            case "zombie", "husk", "parched", "drowned", "zombieVillager" -> 0.22D;
            case "zombifiedPiglin" -> 0.26D;
            case "piglin", "piglinBrute" -> 0.50D;
            case "witherSkeleton" -> 0.45D;
            case "pillager", "vindicator" -> 0.35D;
            default -> 0.24D;
        };
        return Math.max(min, cap);
    }

    public static long currentGunnerDay(Level level) {
        return Math.max(0L, level.getOverworldClockTime() / 24000L);
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    public static float scaleGunnerSpreadMultiplier(Level level, float earlyMultiplier) {
        if (earlyMultiplier <= 0.0F) {
            return earlyMultiplier;
        }

        long day = currentGunnerDay(level);
        int startDay = gunnerAccuracyStartDay();
        if (day <= startDay) {
            return earlyMultiplier;
        }

        int daysToMax = Math.max(1, gunnerAccuracyMaxDay() - startDay);
        double progress = Math.min(1.0D, (double) (day - startDay) / (double) daysToMax);
        double maxSpreadMultiplier = earlyMultiplier * (1.0D - gunnerAccuracyMaxPercent());
        return (float) (earlyMultiplier + (maxSpreadMultiplier - earlyMultiplier) * progress);
    }

    public static int terrorPhantomRapidFireResistanceResetTicks() {
        return Mth.clamp(TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_RESET_TICKS.get(), 1, 200);
    }

    public static int terrorPhantomRapidFireResistanceWarmupHits() {
        return Mth.clamp(TERROR_PHANTOM_RAPID_FIRE_RESISTANCE_WARMUP_HITS.get(), 0, 200);
    }

    public static double terrorPhantomMinigunRapidFireDamageMultiplier() {
        return Mth.clamp(TERROR_PHANTOM_MINIGUN_RAPID_FIRE_DAMAGE_MULTIPLIER.get(), 0.01D, 1.0D);
    }

    public static double terrorPhantomLightMachineGunRapidFireDamageMultiplier() {
        return Mth.clamp(TERROR_PHANTOM_LIGHT_MACHINE_GUN_RAPID_FIRE_DAMAGE_MULTIPLIER.get(), 0.01D, 1.0D);
    }

    public static int terrorRaidGroundWaveCount() {
        return Math.max(1, TERROR_RAID_GROUND_WAVE_COUNT.get());
    }

    public static int terrorRaidAirWaveCount() {
        return Math.max(1, TERROR_RAID_AIR_WAVE_COUNT.get());
    }

    public static boolean factionPatrolEnabled() {
        return FACTION_PATROL_ENABLED.get();
    }

    public static int factionPatrolIntervalDays() {
        return Math.max(0, FACTION_PATROL_INTERVAL_DAYS.get());
    }

    public static int factionPatrolRandomIntervalMinTicks() {
        return Math.max(1, FACTION_PATROL_RANDOM_INTERVAL_MIN_TICKS.get());
    }

    public static int factionPatrolRandomIntervalMaxTicks() {
        return Math.max(factionPatrolRandomIntervalMinTicks(), FACTION_PATROL_RANDOM_INTERVAL_MAX_TICKS.get());
    }

    public static int factionPatrolMinimumDays() {
        return Math.max(0, FACTION_PATROL_MINIMUM_DAYS.get());
    }

    public static int factionPatrolBossBarRange() {
        return Math.max(16, FACTION_PATROL_BOSSBAR_RANGE.get());
    }

    public static double factionPatrolSpawnChance() {
        return clamp01(FACTION_PATROL_SPAWN_CHANCE.get());
    }

    public static boolean factionRaidEnabled() {
        return FACTION_RAID_ENABLED.get();
    }

    public static boolean vehiclesEnabled() {
        return VEHICLE_ENABLED.get();
    }

    public static int factionRaidIntervalDays() {
        return Math.max(0, FACTION_RAID_INTERVAL_DAYS.get());
    }

    public static int factionRaidRandomIntervalMinTicks() {
        return Math.max(1, FACTION_RAID_RANDOM_INTERVAL_MIN_TICKS.get());
    }

    public static int factionRaidRandomIntervalMaxTicks() {
        return Math.max(factionRaidRandomIntervalMinTicks(), FACTION_RAID_RANDOM_INTERVAL_MAX_TICKS.get());
    }

    public static int factionRaidMinimumDays() {
        return Math.max(0, FACTION_RAID_MINIMUM_DAYS.get());
    }

    public static int factionRaidHomeTriggerRadius() {
        return Math.max(8, FACTION_RAID_HOME_TRIGGER_RADIUS.get());
    }

    public static int bulletLifetimeTicks() {
        return Math.max(20, BULLET_LIFETIME_SECONDS.get() * 20);
    }

    public static boolean showCrosshair() {
        return UI_SHOW_CROSSHAIR.get();
    }

    public static boolean showHitFeedback() {
        return UI_SHOW_HIT_FEEDBACK.get();
    }

    public static boolean legacyBulletTrailEnabled() {
        return LEGACY_BULLET_TRAIL_ENABLED.get();
    }

    public static boolean showAmmoHud() {
        return SHOW_AMMO_HUD.get();
    }

    public static boolean showTimersHud() {
        return SHOW_TIMERS_HUD.get();
    }

    public static String crosshair() {
        return CROSSHAIR.get();
    }

    public static boolean showHitmarker() {
        return SHOW_HITMARKER.get();
    }

    public static String dynamicCrosshairDotMode() {
        return DYNAMIC_CROSSHAIR_DOT_MODE.get();
    }

    public static double dynamicCrosshairDotThreshold() {
        return Mth.clamp(DYNAMIC_CROSSHAIR_DOT_THRESHOLD.get(), 0.0D, 90.0D);
    }

    private static double clamp01(double value) {
        if (value < 0.0D) return 0.0D;
        if (value > 1.0D) return 1.0D;
        return value;
    }

    private static double scaledChance(Level level, double baseChance, double maxChance) {
        double base = clamp01(baseChance);
        double cap = clamp01(maxChance);
        if (cap < base) {
            cap = base;
        }

        long day = currentGunnerDay(level);
        int startDay = Math.max(0, SPAWN_SCALING_START_DAY.get());
        int daysToMax = Math.max(1, SPAWN_SCALING_DAYS_TO_MAX.get());

        if (day <= startDay) {
            return base;
        }

        double progress = Math.min(1.0D, (double) (day - startDay) / (double) daysToMax);
        return base + (cap - base) * progress;
    }
}
