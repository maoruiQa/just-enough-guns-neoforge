package ttv.migami.jeg.util;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/**
 * SuperbWarfare {@code CustomExplosion} falloff for entity damage.
 * <p>
 * diameter = radius * 2;
 * distanceRate = dist / diameter;
 * damagePercent = (1 - distanceRate) * seenPercent;
 * damageFinal = (damagePercent^2 + damagePercent) / 2 * damage;
 */
public final class SwStyleExplosion {
    /** Matches SW default config explosion_penetration_ratio = 15 (%). */
    private static final double MIN_SEEN_PERCENT = 0.01D * 15.0D;

    private SwStyleExplosion() {}

    public static void damageEntities(
            ServerLevel level,
            Vec3 center,
            @Nullable Entity source,
            @Nullable Entity owner,
            float damage,
            float radius,
            DamageSource damageSource
    ) {
        if (damage <= 0.0F || radius <= 0.0F) {
            return;
        }
        float diameter = radius * 2.0F;
        AABB area = new AABB(center, center).inflate(diameter + 1.0D);
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        for (Entity entity : level.getEntities(source, area, candidate -> canDamage(candidate, source, owner, ownerVehicle))) {
            double distanceRate = Math.sqrt(entity.distanceToSqr(center)) / (double) diameter;
            if (distanceRate > 1.0D) {
                continue;
            }
            double xDistance = entity.getX() - center.x;
            double yDistance = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - center.y;
            double zDistance = entity.getZ() - center.z;
            double distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);
            if (distance == 0.0D) {
                continue;
            }
            double seenPercent = Mth.clamp(seenPercent(level, center, entity), MIN_SEEN_PERCENT, Double.POSITIVE_INFINITY);
            double damagePercent = (1.0D - distanceRate) * seenPercent;
            float damageFinal = (float) ((damagePercent * damagePercent + damagePercent) / 2.0D * damage);
            if (damageFinal <= 0.5F) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                ModDamageTypes.hurtWithPlayerKillCredit(living, level, damageSource, damageFinal, owner);
            } else {
                entity.hurt(damageSource, damageFinal);
            }
        }
    }

    private static boolean canDamage(
            Entity candidate,
            @Nullable Entity source,
            @Nullable Entity owner,
            @Nullable Entity ownerVehicle
    ) {
        if (!candidate.isAlive() || candidate == source || candidate == owner || candidate == ownerVehicle) {
            return false;
        }
        if (ownerVehicle != null && candidate.getVehicle() == ownerVehicle) {
            return false;
        }
        return candidate instanceof LivingEntity || candidate instanceof VehicleEntity;
    }

    /**
     * Approximate SW {@link Explosion#getSeenPercent} with a single LOS sample to entity center.
     * Full ray fan is unnecessary for balance parity here.
     */
    private static double seenPercent(ServerLevel level, Vec3 center, Entity entity) {
        Vec3 to = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        HitResult hit = level.clip(new ClipContext(center, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() == HitResult.Type.MISS) {
            return 1.0D;
        }
        return MIN_SEEN_PERCENT;
    }
}
