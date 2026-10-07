package ttv.migami.jeg.util;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
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
            if (damageFinal <= 0.0F) {
                continue;
            }
            if (entity instanceof LivingEntity living) {
                float livingDamage = living instanceof Monster ? damageFinal * 1.2F : damageFinal;
                ModDamageTypes.hurtWithPlayerKillCredit(living, level, damageSource, livingDamage, owner == null ? damageSource.getEntity() : owner);
                if (damageSource.is(ModDamageTypes.CUSTOM_EXPLOSION)) {
                    double resistance = Math.max(level.getBlockState(net.minecraft.core.BlockPos.containing(center)).getBlock().getExplosionResistance(),
                            level.getFluidState(net.minecraft.core.BlockPos.containing(center)).getExplosionResistance());
                    double force = damageFinal * .015D - (resistance + .3D) * .3D;
                    living.setDeltaMovement(living.getDeltaMovement().add(ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat.limitImpulse(living, damageSource,
                            center.vectorTo(living.getBoundingBox().getCenter()).normalize().scale(force))));
                    living.setInvulnerableTime(1);
                }
            } else {
                boolean hurt = entity.hurtServer(level, damageSource, damageFinal);
                if (hurt) ttv.migami.jeg.advancement.GameplayActions.hit(owner, source, entity, false);
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

    /** Vanilla exposure ray fan, matching SW CustomExplosion. */
    private static double seenPercent(ServerLevel level, Vec3 center, Entity entity) {
        return net.minecraft.world.level.ServerExplosion.getSeenPercent(center, entity);
    }
}
