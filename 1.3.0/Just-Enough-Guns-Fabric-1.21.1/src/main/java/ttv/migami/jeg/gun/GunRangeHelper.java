package ttv.migami.jeg.gun;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ttv.migami.jeg.Reference;

public final class GunRangeHelper {
    private static final double MIN_PROJECTILE_SPEED = 0.001D;
    private static final int MINIMUM_LIFE_TICKS = 4;
    private static final ResourceLocation FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final ResourceLocation FLARE_GUN_ID = Reference.id("flare_gun");

    private GunRangeHelper() {}

    public static boolean isRangeExempt(GunStats stats) {
        ResourceLocation id = stats.id();
        return id.equals(FLAMETHROWER_ID) || id.equals(FLARE_GUN_ID);
    }

    public static int computeEffectiveLife(GunStats stats) {
        if (isRangeExempt(stats)) {
            return stats.projectileLife();
        }

        GunCategory category = GunCategory.fromStats(stats);
        double projectileSpeed = Math.max(MIN_PROJECTILE_SPEED, stats.projectileSpeed());
        double targetRange = category.maxRange();
        int computed = (int) Math.ceil(targetRange / projectileSpeed);
        return Mth.clamp(computed, MINIMUM_LIFE_TICKS, stats.projectileLife());
    }

    public static double computeEffectiveRange(GunStats stats) {
        int life = computeEffectiveLife(stats);
        double projectileSpeed = Math.max(0.0D, stats.projectileSpeed());
        double rawRange = life * projectileSpeed;

        if (isRangeExempt(stats)) {
            return rawRange;
        }

        double cappedRange = GunCategory.fromStats(stats).maxRange();
        return Math.min(rawRange, cappedRange);
    }
}
