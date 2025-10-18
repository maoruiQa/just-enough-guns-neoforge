package ttv.migami.jeg;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.DoubleValue TERROR_PHANTOM_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PHANTOM_GUNNER_NATURAL_CHANCE;
    public static final ModConfigSpec.DoubleValue PILLAGER_GUNNER_CHANCE;
    public static final ModConfigSpec.DoubleValue SKELETON_GUNNER_CHANCE;

    static {
        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        CLIENT_SPEC = clientBuilder.build();

        ModConfigSpec.Builder serverBuilder = new ModConfigSpec.Builder();

        serverBuilder.push("spawns");
        TERROR_PHANTOM_NATURAL_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Phantom upgrades into a Terror Phantom.")
                .defineInRange("terrorPhantomChance", 0.005D, 0.0D, 1.0D);

        PHANTOM_GUNNER_NATURAL_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Phantom upgrades into a Phantom Gunner when it does not become a Terror Phantom.")
                .defineInRange("phantomGunnerChance", 0.20D, 0.0D, 1.0D);

        PILLAGER_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Pillager immediately converts into a Pillager Gunner variant.")
                .defineInRange("pillagerGunnerChance", 0.20D, 0.0D, 1.0D);

        SKELETON_GUNNER_CHANCE = serverBuilder
                .comment("Probability (0-1) that a naturally spawned Skeleton is flagged to receive a gun on spawn completion.")
                .defineInRange("skeletonGunnerChance", 1.0D / 7.0D, 0.0D, 1.0D);
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

    private static double clamp01(double value) {
        if (value < 0.0D) return 0.0D;
        if (value > 1.0D) return 1.0D;
        return value;
    }
}
