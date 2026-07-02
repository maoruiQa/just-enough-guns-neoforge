package ttv.migami.jeg.faction;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.Config;

public class GunMobValues {
    public static boolean enabled = true;
    public static boolean elitesEnabled = true;
    public static double eliteChance = 0.4D;
    private static final double INITIAL_ELITE_CHANCE = 0.20D;
    private static final double MAX_ELITE_CHANCE = 0.80D;
    private static final long MAX_ELITE_CHANCE_DAY = 100L;
    public static int minDays = 4;
    public static int initialChance = 1;
    public static int chanceIncrement = 1;
    public static int maxChance = 50;

    public static boolean rollElite(Level level, RandomSource random) {
        return elitesEnabled && random.nextDouble() < eliteChance(level);
    }

    public static double eliteChance(Level level) {
        return eliteChanceForDay(Config.currentGunnerDay(level));
    }

    public static double eliteChanceForDay(long day) {
        double progress = Math.min(1.0D, Math.max(0.0D, day / (double) MAX_ELITE_CHANCE_DAY));
        return INITIAL_ELITE_CHANCE + progress * (MAX_ELITE_CHANCE - INITIAL_ELITE_CHANCE);
    }

    public static void init() {
        // TODO: Implement configuration support when server config is added
        // For now using default values
    }
}
