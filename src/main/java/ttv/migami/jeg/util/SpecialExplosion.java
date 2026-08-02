package ttv.migami.jeg.util;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHooks;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class SpecialExplosion {
    public enum Tier { MEDIUM, HUGE }

    private SpecialExplosion() {}

    public static void explode(ServerLevel level, Entity source, Entity owner, float damage, double radius) {
        explode(level, source, owner, damage, radius, Tier.HUGE);
    }

    public static void explode(@Nullable ServerLevel level, @Nullable Entity source, Entity owner, float damage, double radius, Tier tier) {
        if (level == null || source == null) {
            return;
        }
        explodeAt(level, source.position(), owner, damage, radius, tier, source);
    }

    /** Preferred entry for explosives: center is fixed and source may already be discarded. */
    public static void explodeAt(ServerLevel level, Vec3 center, @Nullable Entity owner, float damage, double radius, Tier tier) {
        explodeAt(level, center, owner, damage, radius, tier, null);
    }

    private static void explodeAt(
            @Nullable ServerLevel level,
            Vec3 center,
            @Nullable Entity owner,
            float damage,
            double radius,
            Tier tier,
            @Nullable Entity source
    ) {
        if (level == null || GameTestHooks.isGametestServer()) {
            return;
        }
        if (tier == Tier.HUGE) {
            level.sendParticles(ModParticleTypes.BIG_EXPLOSION.get(), center.x, center.y, center.z, 4, 0.5D, 0.5D, 0.5D, 0.02D);
            level.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), center.x, center.y, center.z, 24, 1.4D, 1.4D, 1.4D, 0.14D);
            level.sendParticles(ModParticleTypes.SMOKE.get(), center.x, center.y, center.z, 18, 1.2D, 1.2D, 1.2D, 0.03D);
        } else {
            level.sendParticles(ModParticleTypes.BIG_EXPLOSION.get(), center.x, center.y, center.z, 2, 0.25D, 0.25D, 0.25D, 0.015D);
            level.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), center.x, center.y, center.z, 14, 0.9D, 0.9D, 0.9D, 0.1D);
        }
        float blockPower = Math.max(2.0F, (float) radius * (tier == Tier.HUGE ? 0.55F : 0.4F));
        // Use null exploder so discarded sources do not re-enter entity hurt loops as the blast origin.
        level.explode(null, center.x, center.y, center.z, blockPower, ExplosionInteraction.MOB);

        AABB area = new AABB(center, center).inflate(radius);
        for (Entity target : level.getEntities(source, area, candidate ->
                candidate != source
                        && (candidate instanceof LivingEntity
                        || candidate instanceof VehicleEntity
                        || candidate instanceof PlacedExplosiveEntity))) {
            if (target instanceof PlacedExplosiveEntity explosive && explosive.isDetonating()) {
                continue;
            }
            Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            double distance = aim.distanceTo(center);
            if (distance > radius || !hasLineOfSight(level, center, target)) {
                continue;
            }
            float scaled = damage * (float) (1.0D - distance / radius);
            if (scaled <= 0.5F) {
                continue;
            }
            if (target instanceof PlacedExplosiveEntity explosive) {
                explosive.hurtFromBlast(scaled, owner);
            } else {
                target.hurt(level.damageSources().explosion(null, owner), scaled);
            }
        }
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 from, Entity target) {
        return level.clip(new net.minecraft.world.level.ClipContext(
                from,
                target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                target
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
}
