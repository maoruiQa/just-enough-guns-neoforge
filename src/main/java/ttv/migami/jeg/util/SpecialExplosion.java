package ttv.migami.jeg.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHooks;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class SpecialExplosion {
    private SpecialExplosion() {}

    public static void explode(ServerLevel level, Entity source, Entity owner, float damage, double radius) {
        if (GameTestHooks.isGametestServer()) {
            return;
        }
        Vec3 center = source.position();
        level.sendParticles(ModParticleTypes.BIG_EXPLOSION.get(), center.x, center.y, center.z, 3, 0.4D, 0.4D, 0.4D, 0.02D);
        level.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), center.x, center.y, center.z, 18, 1.0D, 1.0D, 1.0D, 0.12D);
        level.explode(source, center.x, center.y, center.z, Math.max(2.0F, (float) radius * 0.45F), ExplosionInteraction.MOB);
        AABB area = source.getBoundingBox().inflate(radius);
        for (Entity target : level.getEntities(source, area, candidate -> candidate instanceof LivingEntity || candidate instanceof VehicleEntity)) {
            double distance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(center);
            if (distance > radius || !hasLineOfSight(level, source, target)) {
                continue;
            }
            float scaled = damage * (float) (1.0D - distance / radius);
            if (scaled > 0.5F) {
                target.hurt(level.damageSources().explosion(source, owner), scaled);
            }
        }
    }

    private static boolean hasLineOfSight(ServerLevel level, Entity source, Entity target) {
        return level.clip(new net.minecraft.world.level.ClipContext(
                source.position(),
                target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                source
        )).getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }
}
