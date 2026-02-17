package ttv.migami.jeg;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.world.level.Level;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue LEGACY_BULLET_TRAIL_ENABLED;

    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_MAX_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_MAX_CHANCE;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_MAX_CHANCE;
    public static final ModConfigSpec.DoubleValue SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIE_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue HUSK_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIFIED_PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue WITHER_SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.IntValue SPAWN_SCALING_START_DAY;
    public static final ModConfigSpec.IntValue SPAWN_SCALING_DAYS_TO_MAX;
    public static final ModConfigSpec.BooleanValue RECOIL_BACKSTEP_ENABLED;
    public static final ModConfigSpec.DoubleValue RECOIL_BACKSTEP_SCALE;
    public static final ModConfigSpec.BooleanValue BLOCK_HIT_ANIMATION_ENABLED;
    public static final ModConfigSpec.IntValue BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL;
    public static final ModConfigSpec.IntValue TERROR_RAID_WAVE_INTERVAL_SECONDS;
    public static final ModConfigSpec.IntValue TERROR_RAID_GROUND_WAVE_COUNT;
    public static final ModConfigSpec.IntValue TERROR_RAID_AIR_WAVE_COUNT;
    public static final ModConfigSpec.IntValue BULLET_LIFETIME_SECONDS;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.push("rendering");
        LEGACY_BULLET_TRAIL_ENABLED = clientBuilder
                .comment("If true, use legacy 1.20.1-style bullet trail rendering.")
                .define("legacyBulletTrailEnabled", true);
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();

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
        BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL = serverBuilder
                .comment("Projectile Protection level applied to bound terror phantom (guardian). 0 disables this reduction.")
                .defineInRange("boundTerrorPhantomProjectileProtectionLevel", 2, 0, 20);
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

        serverBuilder.push("combat");
        BULLET_LIFETIME_SECONDS = serverBuilder
                .comment("Global bullet lifetime in seconds. Applies to all bullets.")
                .defineInRange("bulletLifetimeSeconds", 60, 1, 3600);
        serverBuilder.pop();

        SERVER_SPEC = serverBuilder.build();
    }

    private Config() {}

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

    public static int boundTerrorPhantomProjectileProtectionLevel() {
        return Math.max(0, BOUND_TERROR_PHANTOM_PROJECTILE_PROTECTION_LEVEL.get());
    }

    public static int terrorRaidGroundWaveCount() {
        return Math.max(1, TERROR_RAID_GROUND_WAVE_COUNT.get());
    }

    public static int terrorRaidAirWaveCount() {
        return Math.max(1, TERROR_RAID_AIR_WAVE_COUNT.get());
    }

    public static int bulletLifetimeTicks() {
        return Math.max(20, BULLET_LIFETIME_SECONDS.get() * 20);
    }

    public static boolean legacyBulletTrailEnabled() {
        return LEGACY_BULLET_TRAIL_ENABLED.get();
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

        long day = Math.max(0L, level.getDayTime() / 24000L);
        int startDay = Math.max(0, SPAWN_SCALING_START_DAY.get());
        int daysToMax = Math.max(1, SPAWN_SCALING_DAYS_TO_MAX.get());

        if (day <= startDay) {
            return base;
        }

        double progress = Math.min(1.0D, (double) (day - startDay) / (double) daysToMax);
        return base + (cap - base) * progress;
    }
}
