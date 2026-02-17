package ttv.migami.jeg.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.gun.GunRangeHelper;
import ttv.migami.jeg.gun.GunStats;

/**
 * Utility methods shared by gun-wielding AI goals to keep combat behaviour consistent.
 */
public final class GunCombatHelper {
    private static final double DEFAULT_ATTACK_RANGE = 30.0D;
    private static final double MIN_RETREAT_RANGE = 6.0D;
    private static final double MAX_RETREAT_RANGE = 12.0D;

    private GunCombatHelper() {
    }

    public static double resolveAttackRange(GunStats stats) {
        double fallback = DEFAULT_ATTACK_RANGE;
        if (stats != null) {
            double computed = GunRangeHelper.computeEffectiveRange(stats);
            if (computed > 0.0D && !Double.isNaN(computed) && !Double.isInfinite(computed)) {
                fallback = Math.max(fallback, computed);
            }
        }
        return fallback;
    }

    public static double resolveRetreatRange(GunStats stats, double attackRange) {
        double scaled = attackRange * 0.3D;
        double clamped = Math.max(MIN_RETREAT_RANGE, Math.min(MAX_RETREAT_RANGE, scaled));
        if (stats != null && GunRangeHelper.isRangeExempt(stats)) {
            clamped = Math.max(MIN_RETREAT_RANGE, Math.min(MAX_RETREAT_RANGE, attackRange * 0.2D));
        }
        return clamped;
    }

    public static Vec3 computeRetreatPosition(Mob mob, LivingEntity target, double distance) {
        Vec3 away = mob.position().subtract(target.position());
        if (away.lengthSqr() < 1.0E-4D) {
            double dx = mob.getRandom().nextDouble() - 0.5D;
            double dz = mob.getRandom().nextDouble() - 0.5D;
            away = new Vec3(dx, 0.0D, dz);
        }
        Vec3 normalised = away.normalize();
        Vec3 dest = mob.position().add(normalised.scale(distance));
        return new Vec3(dest.x, mob.getY(), dest.z);
    }
}
