package ttv.migami.jeg.gun;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.phys.EntityHitResult;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class GunHeadshotHelper {
    private GunHeadshotHelper() {
    }

    public static boolean isHeadshotTarget(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof VehicleEntity);
    }

    public static boolean isHeadshotHit(EntityHitResult result, LivingEntity target) {
        return isHeadshotTarget(target) && result.getLocation().y >= target.getEyeY() - 0.20D;
    }

    public static float headshotMultiplier(GunStats stats) {
        return headshotMultiplier(stats, null);
    }

    public static float headshotMultiplier(GunStats stats, Entity shooter) {
        if (stats == null) {
            return 1.0F;
        }
        if (!Config.headshotMultiplierEnabled()) {
            return 1.0F;
        }

        String path = stats.id().getPath();
        if (shooter instanceof Skeleton && "bolt_action_rifle".equals(path)) {
            return 1.60F;
        }
        return switch (path) {
            case "bolt_action_rifle" -> 2.00F;
            case "combat_rifle" -> 1.60F;
            case "assault_rifle" -> 1.30F;
            default -> computedMultiplier(stats);
        };
    }

    private static float computedMultiplier(GunStats stats) {
        GunCategory category = GunCategory.fromStats(stats);
        if (category == GunCategory.HEAVY || category == GunCategory.SPECIAL) {
            return 1.00F;
        }

        float effectiveArmorPiercing = BallisticProtection.effectiveArmorPiercing(stats, false);
        double effectiveRange = GunRangeHelper.computeEffectiveRange(stats);
        float apScore = Mth.clamp((effectiveArmorPiercing - 2.0F) / 4.0F, 0.0F, 1.0F);
        float rangeScore = (float) Mth.clamp((effectiveRange - 48.0D) / 72.0D, 0.0D, 1.0D);
        float score = 0.70F * apScore + 0.30F * rangeScore;

        float min;
        float max;
        switch (category) {
            case SMG -> {
                min = 1.10F;
                max = 1.25F;
            }
            case PISTOL -> {
                min = 1.15F;
                max = 1.35F;
            }
            case RIFLE -> {
                min = 1.20F;
                max = 1.60F;
            }
            case SNIPER -> {
                min = 1.55F;
                max = 2.00F;
            }
            case LMG -> {
                min = 1.10F;
                max = 1.35F;
            }
            case SHOTGUN -> {
                min = 1.05F;
                max = 1.20F;
            }
            default -> {
                return 1.00F;
            }
        }

        float multiplier = Mth.lerp(score, min, max);
        return Math.round(multiplier * 100.0F) / 100.0F;
    }
}
