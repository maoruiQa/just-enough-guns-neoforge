package ttv.migami.jeg.gun;

import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;

public final class GunRangeHelper {
    private static final int MINIMUM_LIFE_TICKS = 4;
    private static final ResourceLocation FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final ResourceLocation FLARE_GUN_ID = Reference.id("flare_gun");

    private GunRangeHelper() {}

    public static boolean isRangeExempt(GunStats stats) {
        ResourceLocation id = stats.id();
        return id.equals(FLAMETHROWER_ID) || id.equals(FLARE_GUN_ID);
    }

    public static double getCustomRange(GunStats stats) {
        // No custom ranges - use standard category-based calculations
        return -1.0D;
    }

    public static int computeEffectiveLife(GunStats stats) {
        return Math.max(MINIMUM_LIFE_TICKS, Config.bulletLifetimeTicks());
    }

    public static double computeEffectiveRange(GunStats stats) {
        int life = computeEffectiveLife(stats);
        double projectileSpeed = Math.max(0.0D, stats.projectileSpeed());
        double rawRange = life * projectileSpeed;

        if (isRangeExempt(stats)) {
            return rawRange;
        }

        double customRange = getCustomRange(stats);
        if (customRange > 0.0D) {
            return Math.min(rawRange, customRange);
        }

        double cappedRange = GunCategory.fromStats(stats).maxRange();
        return Math.min(rawRange, cappedRange);
    }
}
