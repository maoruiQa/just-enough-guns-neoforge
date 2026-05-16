package ttv.migami.jeg.vehicle.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;

public final class VehicleMissileEntity extends Entity {
    private static final double DECOY_SEEK_RANGE = 32.0D;

    private int ownerId = -1;
    private int targetId = -1;
    private ResourceLocation weaponId = Reference.id("vehicle_bmp2_missile");

    public VehicleMissileEntity(EntityType<? extends VehicleMissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VehicleMissileEntity(Level level, Entity owner, @Nullable Entity target, Vec3 position, Vec3 velocity, ResourceLocation weaponId) {
        this(ModEntities.VEHICLE_MISSILE.get(), level);
        this.ownerId = owner.getId();
        this.targetId = target == null ? -1 : target.getId();
        this.weaponId = weaponId;
        this.setPos(position);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        VehicleMissileProfile profile = this.profile();
        if (this.tickCount > profile.lifeTicks()) {
            this.explode();
            return;
        }
        if (!this.level().isClientSide) {
            this.steerServer();
            if (this.isRemoved()) {
                return;
            }
            Vec3 nextPosition = this.position().add(this.getDeltaMovement());
            if (this.hitAlongPath(this.position(), nextPosition)) {
                return;
            }
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
        if (this.level().isClientSide) {
            Vec3 motion = this.getDeltaMovement();
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), -motion.x * 0.08D, -motion.y * 0.08D, -motion.z * 0.08D);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), -motion.x * 0.04D, -motion.y * 0.04D, -motion.z * 0.04D);
        }
    }

    private void steerServer() {
        VehicleMissileProfile profile = this.profile();
        Entity target = this.currentTarget();
        if (profile.usesLockOn()) {
            VehicleDecoyEntity.findNearest(this.level(), this.position(), DECOY_SEEK_RANGE).ifPresent(decoy -> this.targetId = decoy.getId());
            target = this.currentTarget();
        }
        Entity owner = this.ownerId < 0 ? null : this.level().getEntity(this.ownerId);
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        boolean validTarget = target instanceof VehicleDecoyEntity
                || (target != null && profile.canContinueTracking(target, owner, ownerVehicle) && !VehicleDecoyEntity.isSmokeBlockingTarget(target));
        if (profile.usesLockOn() && target != null && validTarget) {
            this.warnTrackedTarget(target);
            Vec3 desired = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(this.position()).normalize().scale(profile.maxSpeed());
            this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - profile.turnRate()).add(desired.scale(profile.turnRate())));
            if (this.distanceToSqr(target) < 1.4D) {
                this.explode();
            }
        } else if (profile.usesWireGuidance() && owner instanceof LivingEntity livingOwner) {
            Vec3 desired = livingOwner.getViewVector(1.0F).normalize().scale(profile.maxSpeed());
            this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - profile.turnRate()).add(desired.scale(profile.turnRate())));
        }
    }

    private void warnTrackedTarget(Entity target) {
        int interval = Math.max(2, (int) (0.04D * this.distanceTo(target)));
        if (this.tickCount % interval != 0) {
            return;
        }
        VehicleEntity.warnIncomingMissileTarget(target);
    }

    private boolean hitAlongPath(Vec3 start, Vec3 end) {
        HitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.setPos(blockHit.getLocation());
            this.explode();
            return true;
        }

        Entity owner = this.ownerId < 0 ? null : this.level().getEntity(this.ownerId);
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        AABB path = new AABB(start, end).inflate(0.55D);
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Entity target : this.level().getEntities(this, path, target -> this.canImpact(target, owner, ownerVehicle))) {
            double distance = target.distanceToSqr(start);
            if (distance < closestDistance) {
                closest = target;
                closestDistance = distance;
            }
        }
        if (closest != null) {
            this.setPos(closest.position().add(0.0D, closest.getBbHeight() * 0.5D, 0.0D));
            this.explode();
            return true;
        }
        return false;
    }

    private boolean canImpact(Entity target, @Nullable Entity owner, @Nullable Entity ownerVehicle) {
        return target.isAlive()
                && target != owner
                && target != ownerVehicle
                && target.getVehicle() != ownerVehicle
                && (target instanceof LivingEntity || target instanceof VehicleEntity);
    }

    @Nullable
    private Entity currentTarget() {
        return this.targetId < 0 ? null : this.level().getEntity(this.targetId);
    }

    private void explode() {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            VehicleMissileProfile profile = this.profile();
            serverLevel.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 8, 0.4D, 0.4D, 0.4D, 0.06D);
            Entity owner = this.ownerId < 0 ? null : this.level().getEntity(this.ownerId);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), profile.explosionPower(), ExplosionInteraction.MOB);
            Entity ownerVehicle = owner == null ? null : owner.getVehicle();
            for (VehicleEntity target : this.level().getEntitiesOfClass(VehicleEntity.class, this.getBoundingBox().inflate(profile.blastRadius()))) {
                if (target != ownerVehicle) {
                    target.hurt(this.damageSources().explosion(this, owner), profile.vehicleDamage());
                }
            }
            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(profile.blastRadius()))) {
                if (target != owner) {
                    target.hurt(this.damageSources().explosion(this, owner), profile.livingDamage());
                }
            }
        }
        this.discard();
    }

    private VehicleMissileProfile profile() {
        return VehicleMissileProfile.get(this.weaponId);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ownerId = tag.getInt("OwnerId");
        this.targetId = tag.getInt("TargetId");
        if (tag.contains("WeaponId")) {
            this.weaponId = ResourceLocation.parse(tag.getString("WeaponId"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", this.ownerId);
        tag.putInt("TargetId", this.targetId);
        tag.putString("WeaponId", this.weaponId.toString());
    }
}
