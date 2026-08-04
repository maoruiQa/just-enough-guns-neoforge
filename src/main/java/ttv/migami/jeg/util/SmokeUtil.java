package ttv.migami.jeg.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;

/**
 * Smoke lock denial. Volumes come from {@link SmokeCloudTracker} (reported each tick by
 * smoke decoys and actively smoking grenades on both logical sides).
 */
public final class SmokeUtil {
    private static final int SMOKED_DURATION = 40;

    private SmokeUtil() {}

    public static AABB smokeVolume(Entity source) {
        return SmokeCloudTracker.pointInSmoke(source.level(), source.position())
                ? new AABB(
                        source.getX() - SmokeCloudTracker.HALF_WIDTH,
                        source.getY() - 0.5D,
                        source.getZ() - SmokeCloudTracker.HALF_WIDTH,
                        source.getX() + SmokeCloudTracker.HALF_WIDTH,
                        source.getY() + SmokeCloudTracker.HEIGHT,
                        source.getZ() + SmokeCloudTracker.HALF_WIDTH)
                : new AABB(
                        source.getX() - SmokeCloudTracker.HALF_WIDTH,
                        source.getY() - 0.5D,
                        source.getZ() - SmokeCloudTracker.HALF_WIDTH,
                        source.getX() + SmokeCloudTracker.HALF_WIDTH,
                        source.getY() + SmokeCloudTracker.HEIGHT,
                        source.getZ() + SmokeCloudTracker.HALF_WIDTH);
    }

    public static boolean isInSmoke(Entity entity) {
        if (entity == null || entity.level() == null) {
            return false;
        }
        if (entity instanceof VehicleDecoyEntity decoy && decoy.isSmokeDecoy()) {
            return true;
        }
        Vec3 mid = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 head = entity.position().add(0.0D, entity.getBbHeight() * 0.9D, 0.0D);
        return SmokeCloudTracker.pointInSmoke(entity.level(), mid)
                || SmokeCloudTracker.pointInSmoke(entity.level(), head)
                || SmokeCloudTracker.boxIntersectsSmoke(entity.level(), entity.getBoundingBox());
    }

    public static boolean isSmokeBlockingTarget(Entity target) {
        return isInSmoke(target);
    }

    public static boolean isSmokeBlockingLock(Entity seeker, Entity target) {
        if (target == null) {
            return false;
        }
        if (isInSmoke(target) || (seeker != null && isInSmoke(seeker))) {
            return true;
        }
        if (seeker == null || seeker.level() != target.level()) {
            return false;
        }
        Vec3 from = seeker instanceof LivingEntity living
                ? living.getEyePosition()
                : seeker.position().add(0.0D, seeker.getBbHeight() * 0.5D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        return isLineOccludedBySmoke(seeker.level(), from, to);
    }

    public static boolean isSmokeBlockingLock(Level level, Vec3 from, Entity target) {
        if (target == null || level == null || from == null) {
            return false;
        }
        if (isInSmoke(target)) {
            return true;
        }
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        return isLineOccludedBySmoke(level, from, to);
    }

    public static boolean isLineOccludedBySmoke(Level level, Vec3 from, Vec3 to) {
        return SmokeCloudTracker.lineOccluded(level, from, to);
    }

    public static void applySmokedNearby(Entity decoy) {
        if (decoy.level().isClientSide() || !(decoy instanceof VehicleDecoyEntity smoke) || !smoke.isSmokeDecoy()) {
            return;
        }
        AABB area = new AABB(
                decoy.getX() - SmokeCloudTracker.HALF_WIDTH,
                decoy.getY() - 0.5D,
                decoy.getZ() - SmokeCloudTracker.HALF_WIDTH,
                decoy.getX() + SmokeCloudTracker.HALF_WIDTH,
                decoy.getY() + SmokeCloudTracker.HEIGHT,
                decoy.getZ() + SmokeCloudTracker.HALF_WIDTH
        );
        for (LivingEntity living : decoy.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (living.isAlive()) {
                living.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SMOKED.get()),
                        SMOKED_DURATION, 0, false, false, true));
            }
        }
    }
}
