package ttv.migami.jeg.vehicle.projectile;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleMissileEntity extends Entity {
    private static final int LIFE_TICKS = 120;
    private static final double SEEK_RANGE = 18.0D;
    private static final double MAX_SPEED = 0.95D;
    private static final double TURN_RATE = 0.16D;

    private int ownerId = -1;
    private int targetId = -1;

    public VehicleMissileEntity(EntityType<? extends VehicleMissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VehicleMissileEntity(Level level, Entity owner, @Nullable Entity target, Vec3 position, Vec3 velocity) {
        this(ModEntities.VEHICLE_MISSILE.get(), level);
        this.ownerId = owner.getId();
        this.targetId = target == null ? -1 : target.getId();
        this.setPos(position);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFE_TICKS) {
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
        Entity target = this.currentTarget();
        VehicleDecoyEntity.findNearest(this.level(), this.position(), SEEK_RANGE).ifPresent(decoy -> this.targetId = decoy.getId());
        target = this.currentTarget();
        if (target != null && target.isAlive()) {
            this.warnTrackedTarget(target);
            Vec3 desired = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(this.position()).normalize().scale(MAX_SPEED);
            this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - TURN_RATE).add(desired.scale(TURN_RATE)));
            if (this.distanceToSqr(target) < 1.4D) {
                this.explode();
            }
        }
    }

    private void warnTrackedTarget(Entity target) {
        if (!(target instanceof ServerPlayer player) || this.tickCount % 20 != 0) {
            return;
        }
        player.displayClientMessage(Component.translatable("message.jeg.vehicle.lock_warning"), true);
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING,
                SoundSource.PLAYERS,
                0.8F,
                1.7F
        );
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
            serverLevel.sendParticles(ModParticleTypes.SMALL_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 8, 0.4D, 0.4D, 0.4D, 0.06D);
            Entity owner = this.ownerId < 0 ? null : this.level().getEntity(this.ownerId);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.4F, ExplosionInteraction.MOB);
            Entity ownerVehicle = owner == null ? null : owner.getVehicle();
            for (VehicleEntity target : this.level().getEntitiesOfClass(VehicleEntity.class, this.getBoundingBox().inflate(3.5D))) {
                if (target != ownerVehicle) {
                    target.hurt(this.damageSources().explosion(this, owner), 18.0F);
                }
            }
            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D))) {
                if (target != owner) {
                    target.hurt(this.damageSources().explosion(this, owner), 12.0F);
                }
            }
        }
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ownerId = tag.getInt("OwnerId");
        this.targetId = tag.getInt("TargetId");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("OwnerId", this.ownerId);
        tag.putInt("TargetId", this.targetId);
    }
}
