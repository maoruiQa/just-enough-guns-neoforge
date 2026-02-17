package ttv.migami.jeg;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIE_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue HUSK_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue ZOMBIFIED_PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue PIGLIN_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue WITHER_SKELETON_GUNNER_CHANCE;
    public static final ModConfigSpec.IntValue TERROR_RAID_WAVE_INTERVAL_SECONDS;
    public static final ModConfigSpec.IntValue TERROR_RAID_GROUND_WAVE_COUNT;
    public static final ModConfigSpec.IntValue TERROR_RAID_AIR_WAVE_COUNT;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();

        serverBuilder.push("spawns");
        TERROR_PHANTOM_NATURAL_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Phantom upgrades into a Terror Phantom.")
                .defineInRange("terrorPhantomChance", 0.03D, 0.0D, 1.0D);

        PHANTOM_GUNNER_NATURAL_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Phantom upgrades into a Phantom Gunner when it does not become a Terror Phantom.")
                .defineInRange("phantomGunnerChance", 0.20D, 0.0D, 1.0D);

        PILLAGER_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Pillager immediately converts into a Pillager Gunner variant.")
                .defineInRange("pillagerGunnerChance", 0.20D, 0.0D, 1.0D);

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

        SERVER_SPEC = serverBuilder.build();
    }

    private Config() {}

    public static double terrorPhantomChance() {
        return clamp01(TERROR_PHANTOM_NATURAL_CHANCE.get());
    }

    public static double phantomGunnerChance() {
        return clamp01(PHANTOM_GUNNER_NATURAL_CHANCE.get());
    }

    public static double pillagerGunnerChance() {
        return clamp01(PILLAGER_GUNNER_CHANCE.get());
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

    public static int terrorRaidGroundWaveCount() {
        return Math.max(1, TERROR_RAID_GROUND_WAVE_COUNT.get());
    }

    public static int terrorRaidAirWaveCount() {
        return Math.max(1, TERROR_RAID_AIR_WAVE_COUNT.get());
    }

    private static double clamp01(double value) {
        if (value < 0.0D) return 0.0D;
        if (value > 1.0D) return 1.0D;
        return value;
    }
}
