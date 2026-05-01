package ttv.migami.jeg.gun;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import ttv.migami.jeg.Reference;

public final class GunRangeHelper {
    private static final double MIN_PROJECTILE_SPEED = 0.001D;
    private static final int MINIMUM_LIFE_TICKS = 4;
    private static final double RANGE_MULTIPLIER = 1.2D;
    private static final Identifier FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final Identifier FLARE_GUN_ID = Reference.id("flare_gun");

    private GunRangeHelper() {}

    public static boolean isRangeExempt(GunStats stats) {
        Identifier id = stats.id();
        return id.equals(FLAMETHROWER_ID) || id.equals(FLARE_GUN_ID);
    }

    public static double getCustomRange(GunStats stats) {
        // No custom ranges - use standard category-based calculations
        return -1.0D;
    }

    public static int computeEffectiveLife(GunStats stats) {
        int scaledLifeCap = Math.max(MINIMUM_LIFE_TICKS, (int) Math.ceil(stats.projectileLife() * RANGE_MULTIPLIER));
        if (isRangeExempt(stats)) {
            return scaledLifeCap;
        }

        double customRange = getCustomRange(stats);
        if (customRange > 0.0D) {
            double projectileSpeed = Math.max(MIN_PROJECTILE_SPEED, stats.projectileSpeed());
            int computed = (int) Math.ceil((customRange * RANGE_MULTIPLIER) / projectileSpeed);
            return Mth.clamp(computed, MINIMUM_LIFE_TICKS, scaledLifeCap);
        }

        GunCategory category = GunCategory.fromStats(stats);
        double projectileSpeed = Math.max(MIN_PROJECTILE_SPEED, stats.projectileSpeed());
        double targetRange = category.maxRange() * RANGE_MULTIPLIER;
        int computed = (int) Math.ceil(targetRange / projectileSpeed);
        return Mth.clamp(computed, MINIMUM_LIFE_TICKS, scaledLifeCap);
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
            return Math.min(rawRange, customRange * RANGE_MULTIPLIER);
        }

        double cappedRange = GunCategory.fromStats(stats).maxRange() * RANGE_MULTIPLIER;
        return Math.min(rawRange, cappedRange);
    }
}
