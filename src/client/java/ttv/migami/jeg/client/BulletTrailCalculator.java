package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.client.render.BulletTrailRenderer;
import ttv.migami.jeg.gun.BulletPenetrationHelper;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.GunRangeHelper;

/**
 * Client-side bullet trail calculator for instant hitscan weapons.
 * Performs raycast to calculate complete bullet path including penetration.
 */
public class BulletTrailCalculator {

    /**
     * Calculate and render instant trail for fast bullets.
     * Called on client side when gun is fired.
     */
    public static void calculateInstantTrail(Vec3 start, Vec3 direction, GunStats stats, Entity shooter) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        double maxRange = GunRangeHelper.computeEffectiveRange(stats); // Use GunRangeHelper for range calculation
        Vec3 motion = direction.scale(maxRange);
        Vec3 searchStart = start;
        Vec3 trailStart = start;

        int color = stats.trailColor();
        float size = stats.clampedProjectileSize();

        // Loop to handle penetration
        int maxIterations = 20;
        for (int i = 0; i < maxIterations; i++) {
            Vec3 searchEnd = searchStart.add(motion);

            // Check entity collision first
            EntityHitResult entityHit = findEntityHit(level, searchStart, searchEnd, shooter);
            if (entityHit != null) {
                // Hit entity - add trail to hit point and stop
                BulletTrailRenderer.addInstantTrail(trailStart, entityHit.getLocation(), color, size);
                return;
            }

            // Check block collision
            ClipContext clipContext = new ClipContext(
                searchStart,
                searchEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
            );
            BlockHitResult blockHit = level.clip(clipContext);

            if (blockHit.getType() != HitResult.Type.BLOCK) {
                // No more collisions - add final trail segment
                BulletTrailRenderer.addInstantTrail(trailStart, searchEnd, color, size);
                return;
            }

            BlockPos hitPos = blockHit.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);
            Vec3 hitLocation = blockHit.getLocation();

            boolean isPenetrable = BulletPenetrationHelper.isPenetrable(level, hitState);

            if (isPenetrable) {
                // Penetrable block - calculate exit point and continue
                Vec3 dir = motion.normalize();
                double distanceToHit = searchStart.distanceTo(hitLocation);
                double remainingDistance = searchStart.distanceTo(searchEnd) - distanceToHit;

                Vec3 exitPoint = new Vec3(
                    hitPos.getX() + 0.5 + dir.x * 0.6,
                    hitPos.getY() + 0.5 + dir.y * 0.6,
                    hitPos.getZ() + 0.5 + dir.z * 0.6
                );

                // Add trail segment through penetrable block
                BulletTrailRenderer.addInstantTrail(trailStart, exitPoint, color, size);

                // Update for next iteration
                searchStart = exitPoint;
                motion = dir.scale(remainingDistance);
                trailStart = exitPoint;
            } else {
                // Solid block - add final trail and stop
                BulletTrailRenderer.addInstantTrail(trailStart, hitLocation, color, size);
                return;
            }
        }
    }

    /**
     * Find entity hit along ray.
     */
    private static EntityHitResult findEntityHit(Level level, Vec3 start, Vec3 end, Entity shooter) {
        Vec3 direction = end.subtract(start);
        double distance = direction.length();

        EntityHitResult closest = null;
        double closestDistance = distance;

        for (Entity entity : level.getEntities(shooter, shooter.getBoundingBox().inflate(distance))) {
            if (entity == shooter) {
                continue;
            }

            // Simple bounding box check
            Vec3 entityPos = entity.position();
            double distToEntity = start.distanceTo(entityPos);

            if (distToEntity < closestDistance) {
                // Check if ray intersects entity bounding box
                if (entity.getBoundingBox().clip(start, end).isPresent()) {
                    closestDistance = distToEntity;
                    closest = new EntityHitResult(entity, entityPos);
                }
            }
        }

        return closest;
    }
}
