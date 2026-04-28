package ttv.migami.jeg;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.config.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue LEGACY_BULLET_TRAIL_ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_AMMO_HUD;
    public static final ModConfigSpec.BooleanValue SHOW_TIMERS_HUD;
    public static final ModConfigSpec.StringValue CROSSHAIR;
    public static final ModConfigSpec.BooleanValue SHOW_HITMARKER;
    public static final ModConfigSpec.StringValue DYNAMIC_CROSSHAIR_DOT_MODE;
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
    public static final ModConfigSpec.BooleanValue GUNNER_TERRAIN_INTERACTION_ENABLED;
    public static final ModConfigSpec.IntValue GUNNER_TERRAIN_INTERACTION_MAX_TIER;
    public static final ModConfigSpec.IntValue GUNNER_ACCURACY_START_DAY;
    public static final ModConfigSpec.IntValue GUNNER_ACCURACY_DAYS_TO_MAX;
    public static final ModConfigSpec.DoubleValue GUNNER_ACCURACY_MAX_SPREAD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GUNNER_SHOTGUN_SPREAD_MULTIPLIER;
    public static final ModConfigSpec.IntValue BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL;
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
    public static final ModConfigSpec.IntValue BULLET_LIFETIME_SECONDS;
    public static final ModConfigSpec.BooleanValue UI_SHOW_CROSSHAIR;
    public static final ModConfigSpec.BooleanValue UI_SHOW_HIT_FEEDBACK;
    private static final Path CLIENT_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(Reference.MOD_ID + "-client.toml");
    private static final Path SERVER_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(Reference.MOD_ID + "-server.toml");
    private static final Map<String, ModConfigSpec.Value<?>> COMMAND_CONFIGS = new LinkedHashMap<>();

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
        GUNNER_TERRAIN_INTERACTION_ENABLED = serverBuilder
                .comment("If true, ground gunners can break and place low-tier blocks to get around simple terrain obstacles.")
                .define("gunnerTerrainInteractionEnabled", true);
        GUNNER_TERRAIN_INTERACTION_MAX_TIER = serverBuilder
                .comment("Maximum bulletproof tier ground gunners are allowed to break while traversing terrain. 0 = penetrable only, 2 = tier 2 and below.")
                .defineInRange("gunnerTerrainInteractionMaxTier", 2, 0, 3);
        GUNNER_ACCURACY_START_DAY = serverBuilder
                .comment("In-game day when gunner accuracy scaling starts.")
                .defineInRange("gunnerAccuracyStartDay", 5, 0, 5000);
        GUNNER_ACCURACY_DAYS_TO_MAX = serverBuilder
                .comment("In-game days needed for gunner accuracy to reach configured max value.")
                .defineInRange("gunnerAccuracyDaysToMax", 40, 1, 5000);
        GUNNER_ACCURACY_MAX_SPREAD_MULTIPLIER = serverBuilder
                .comment("Late-game gunner spread multiplier. Lower = more accurate. Typical range: 1.5-4.0.")
                .defineInRange("gunnerAccuracyMaxSpreadMultiplier", 2.5D, 0.1D, 20.0D);
        GUNNER_SHOTGUN_SPREAD_MULTIPLIER = serverBuilder
                .comment("Additional spread multiplier for gunner-fired shotguns. Lower = tighter pellet grouping.")
                .defineInRange("gunnerShotgunSpreadMultiplier", 0.82D, 0.2D, 1.0D);
        BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL = serverBuilder
                .comment("Projectile Protection level applied to bound terror phantom (guardian). 0 disables this reduction.")
                .defineInRange("boundTerrorPhantomProjectileProtectionLevel", 5, 0, 20);
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
        registerCommandConfig("mob.terrorPhantom.chance", TERROR_PHANTOM_NATURAL_CHANCE);
        registerCommandConfig("mob.terrorPhantom.maxChance", TERROR_PHANTOM_MAX_CHANCE);
        registerCommandConfig("mob.phantomGunner.chance", PHANTOM_GUNNER_NATURAL_CHANCE);
        registerCommandConfig("mob.phantomGunner.maxChance", PHANTOM_GUNNER_MAX_CHANCE);
        registerCommandConfig("mob.phantomGunner.deathExplosion", PHANTOM_GUNNER_DEATH_EXPLOSION_ENABLED);
        registerCommandConfig("mob.pillagerGunner.chance", PILLAGER_GUNNER_CHANCE);
        registerCommandConfig("mob.pillagerGunner.maxChance", PILLAGER_GUNNER_MAX_CHANCE);
        registerCommandConfig("mob.skeletonGunner.chance", SKELETON_GUNNER_CHANCE);
        registerCommandConfig("mob.zombieGunner.chance", ZOMBIE_GUNNER_CHANCE);
        registerCommandConfig("mob.huskGunner.chance", HUSK_GUNNER_CHANCE);
        registerCommandConfig("mob.parchedGunner.chance", PARCHED_GUNNER_CHANCE);
        registerCommandConfig("mob.zombifiedPiglinGunner.chance", ZOMBIFIED_PIGLIN_GUNNER_CHANCE);
        registerCommandConfig("mob.piglinGunner.chance", PIGLIN_GUNNER_CHANCE);
        registerCommandConfig("mob.witherSkeletonGunner.chance", WITHER_SKELETON_GUNNER_CHANCE);
        registerCommandConfig("combat.bulletBlockDestruction", BULLET_BLOCK_DESTRUCTION_ENABLED);
        registerCommandConfig("combat.magazineFeed", MAGAZINE_FEED_ENABLED);
        registerCommandConfig("combat.gunnerTerrain.enabled", GUNNER_TERRAIN_INTERACTION_ENABLED);
        registerCommandConfig("combat.gunnerTerrain.maxTier", GUNNER_TERRAIN_INTERACTION_MAX_TIER);
    }

    private Config() {}

    public static void load() {
        CLIENT_SPEC.load(CLIENT_CONFIG_PATH);
        SERVER_SPEC.load(SERVER_CONFIG_PATH);
    }

    public static void saveServerConfig() {
        SERVER_SPEC.save(SERVER_CONFIG_PATH);
    }

    public static Object getConfigValue(String key) {
        ModConfigSpec.Value<?> value = COMMAND_CONFIGS.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown config key: " + key);
        }
        return value.get();
    }

    public static void setConfigValue(String key, Object rawValue) {
        ModConfigSpec.Value<?> value = COMMAND_CONFIGS.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown config key: " + key);
        }
        setValue(value, rawValue);
    }

    private static void registerCommandConfig(String key, ModConfigSpec.Value<?> value) {
        COMMAND_CONFIGS.put(key, value);
    }

    private static void setValue(ModConfigSpec.Value<?> value, Object rawValue) {
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
        throw new IllegalArgumentException("Unsupported config value type for " + value.path());
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

    public static boolean gunnerTerrainInteractionEnabled() {
        return GUNNER_TERRAIN_INTERACTION_ENABLED.get();
    }

    public static int gunnerTerrainInteractionMaxTier() {
        return Mth.clamp(GUNNER_TERRAIN_INTERACTION_MAX_TIER.get(), 0, 3);
    }

    public static void setBulletBlockDestructionEnabled(boolean enabled) {
        setConfigValue("combat.bulletBlockDestruction", enabled);
        saveServerConfig();
    }

    public static int gunnerAccuracyStartDay() {
        return Math.max(0, GUNNER_ACCURACY_START_DAY.get());
    }

    public static int gunnerAccuracyDaysToMax() {
        return Math.max(1, GUNNER_ACCURACY_DAYS_TO_MAX.get());
    }

    public static double gunnerAccuracyMaxSpreadMultiplier() {
        return Math.max(0.1D, GUNNER_ACCURACY_MAX_SPREAD_MULTIPLIER.get());
    }

    public static double gunnerShotgunSpreadMultiplier() {
        return Mth.clamp(GUNNER_SHOTGUN_SPREAD_MULTIPLIER.get(), 0.2D, 1.0D);
    }

    public static float scaleGunnerSpreadMultiplier(Level level, float earlyMultiplier) {
        if (earlyMultiplier <= 0.0F) {
            return earlyMultiplier;
        }

        long day = Math.max(0L, level.getGameTime() / 24000L);
        int startDay = gunnerAccuracyStartDay();
        if (day <= startDay) {
            return earlyMultiplier;
        }

        int daysToMax = gunnerAccuracyDaysToMax();
        double progress = Math.min(1.0D, (double) (day - startDay) / (double) daysToMax);
        double maxSpreadMultiplier = Math.min((double) earlyMultiplier, gunnerAccuracyMaxSpreadMultiplier());
        return (float) (earlyMultiplier + (maxSpreadMultiplier - earlyMultiplier) * progress);
    }

    public static int boundTerrorPhantomProjectileProtectionLevel() {
        return Math.max(0, BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL.get());
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

        long day = Math.max(0L, level.getGameTime() / 24000L);
        int startDay = Math.max(0, SPAWN_SCALING_START_DAY.get());
        int daysToMax = Math.max(1, SPAWN_SCALING_DAYS_TO_MAX.get());

        if (day <= startDay) {
            return base;
        }

        double progress = Math.min(1.0D, (double) (day - startDay) / (double) daysToMax);
        return base + (cap - base) * progress;
    }
}
