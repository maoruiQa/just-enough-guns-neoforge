package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public final class VehiclePickMixin {
    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), cancellable = true)
    private static void jeg$projectileVehicleParts(Level level, Entity source, Vec3 from, Vec3 to, AABB search,
                                                    Predicate<Entity> predicate, float margin,
                                                    CallbackInfoReturnable<EntityHitResult> result) {
        List<Entity> candidates = level.getEntities(source, search.inflate(16.0D), predicate);
        if (candidates.stream().noneMatch(entity -> entity instanceof VehicleEntity)) return;
        Entity nearestEntity = null;
        Vec3 nearestPoint = null;
        double nearest = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            if (source instanceof Projectile projectile && (projectile.getOwner() == entity || entity.getPassengers().contains(projectile.getOwner()))) continue;
            Vec3 point;
            if (entity instanceof VehicleEntity vehicle) {
                if (!VehicleGeometry.bounds(vehicle).intersects(search)) continue;
                VehicleGeometry.Hit hit = VehicleGeometry.clip(vehicle, from, to);
                if (hit == null) continue;
                point = hit.position();
            } else {
                Optional<Vec3> clipped = entity.getBoundingBox().inflate(margin).clip(from, to);
                if (clipped.isEmpty()) continue;
                point = clipped.get();
            }
            double distance = from.distanceToSqr(point);
            if (distance < nearest) {
                nearest = distance;
                nearestEntity = entity;
                nearestPoint = point;
            }
        }
        result.setReturnValue(nearestEntity == null ? null : new EntityHitResult(nearestEntity, nearestPoint));
    }

    @Inject(method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At("HEAD"), cancellable = true)
    private static void jeg$pickVehicleParts(Entity source, Vec3 from, Vec3 to, AABB search, Predicate<Entity> predicate,
                                              double maxDistance, CallbackInfoReturnable<EntityHitResult> result) {
        List<Entity> candidates = source.level().getEntities(source, search.inflate(16.0D), predicate);
        if (candidates.stream().noneMatch(entity -> entity instanceof VehicleEntity)) return;

        Entity nearestEntity = null;
        Vec3 nearestPoint = null;
        double nearest = maxDistance;
        for (Entity entity : candidates) {
            Vec3 point;
            if (entity instanceof VehicleEntity vehicle) {
                if (!VehicleGeometry.bounds(vehicle).intersects(search)) continue;
                VehicleGeometry.Hit hit = VehicleGeometry.clip(vehicle, from, to);
                if (hit == null) continue;
                point = hit.position();
            } else {
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                Optional<Vec3> clipped = box.clip(from, to);
                if (box.contains(from)) {
                    if (nearest >= 0.0D) {
                        nearestEntity = entity;
                        nearestPoint = clipped.orElse(from);
                        nearest = 0.0D;
                    }
                    continue;
                }
                if (clipped.isEmpty()) continue;
                point = clipped.get();
            }
            double distance = from.distanceToSqr(point);
            if (distance < nearest || nearest == 0.0D) {
                if (entity.getRootVehicle() == source.getRootVehicle()) {
                    if (nearest != 0.0D) continue;
                } else {
                    nearest = distance;
                }
                nearestEntity = entity;
                nearestPoint = point;
            }
        }
        result.setReturnValue(nearestEntity == null ? null : new EntityHitResult(nearestEntity, nearestPoint));
    }
}
