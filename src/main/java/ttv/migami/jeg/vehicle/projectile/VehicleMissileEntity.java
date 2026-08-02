package ttv.migami.jeg.vehicle.projectile;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;

public final class VehicleMissileEntity extends Entity {
    private static final double DECOY_SEEK_RANGE = 32.0D;
    private static final float VEHICLE_DIRECT_HIT_BLOCK_DAMAGE_SCALE = 0.3F;
    private static final double VEHICLE_DIRECT_HIT_FALLOFF_DISTANCE_SCALE = 1.7D;
    private static final int BMP2_NO_DROP_TICKS = 5;

    private int ownerId = -1;
    private int targetId = -1;
    private int directHitVehicleId = -1;
    private int directHitEntityId = -1;
    private Identifier weaponId = Reference.id("vehicle_bmp2_missile");
    @Nullable
    private Vec3 targetPosition;
    private boolean topAttack;

    public VehicleMissileEntity(EntityType<? extends VehicleMissileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VehicleMissileEntity(Level level, Entity owner, @Nullable Entity target, Vec3 position, Vec3 velocity, Identifier weaponId) {
        this(level, owner, target, null, position, velocity, weaponId, false);
    }

    public VehicleMissileEntity(
            Level level,
            Entity owner,
            @Nullable Entity target,
            @Nullable Vec3 targetPosition,
            Vec3 position,
            Vec3 velocity,
            Identifier weaponId,
            boolean topAttack
    ) {
        this(ModEntities.VEHICLE_MISSILE.get(), level);
        this.ownerId = owner.getId();
        this.targetId = target == null ? -1 : target.getId();
        this.weaponId = weaponId;
        this.targetPosition = targetPosition;
        this.topAttack = topAttack;
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
        if (!this.level().isClientSide()) {
            this.steerServer();
            this.setDeltaMovement(this.flattenBmp2StartupDrop(this.getDeltaMovement()));
            if (this.isRemoved()) {
                return;
            }
            Vec3 nextPosition = this.position().add(this.getDeltaMovement());
            if (this.hitAlongPath(this.position(), nextPosition)) {
                return;
            }
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
        if (this.level().isClientSide()) {
            Vec3 motion = this.getDeltaMovement();
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), -motion.x * 0.08D, -motion.y * 0.08D, -motion.z * 0.08D);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), -motion.x * 0.04D, -motion.y * 0.04D, -motion.z * 0.04D);
        }
    }

    private Vec3 flattenBmp2StartupDrop(Vec3 motion) {
        if (!Reference.id("vehicle_bmp2_missile").equals(this.weaponId) || this.tickCount > BMP2_NO_DROP_TICKS || motion.y >= 0.0D) {
            return motion;
        }
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return Vec3.ZERO;
        }
        return horizontal.normalize().scale(motion.length());
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
        Vec3 aimPoint = target != null
                ? target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D)
                : this.targetPosition;
        // SW Igla: lose active guidance if owner stops zooming / loses LOS
        if (Reference.id("igla_9k38").equals(this.weaponId) && owner instanceof ServerPlayer player) {
            boolean stillAiming = NetworkHandler.isAiming(player);
            boolean los = target == null || player.hasLineOfSight(target);
            if (!stillAiming || !los) {
                validTarget = false;
                this.targetId = -1;
                aimPoint = this.targetPosition;
            }
        }

        if (profile.usesLockOn() && aimPoint != null && (target == null || validTarget)) {
            if (target != null && this.targetId >= 0) {
                this.warnTrackedTarget(target);
            }
            boolean javelinStyle = Reference.id("javelin").equals(this.weaponId)
                    || Reference.id("igla_9k38").equals(this.weaponId);
            if (javelinStyle) {
                this.steerJavelinStyle(profile, this.targetId >= 0 ? target : null, aimPoint);
            } else {
                if (this.topAttack && this.tickCount < 30) {
                    aimPoint = aimPoint.add(0.0D, 20.0D, 0.0D);
                }
                Vec3 desired = aimPoint.subtract(this.position()).normalize().scale(profile.maxSpeed());
                this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - profile.turnRate()).add(desired.scale(profile.turnRate())));
                if (target != null && this.distanceToSqr(target) < 1.4D) {
                    this.directHitEntityId = target.getId();
                    if (target instanceof VehicleEntity) {
                        this.directHitVehicleId = target.getId();
                        this.setPos(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
                    }
                    this.explode();
                } else if (target == null && (!this.topAttack || this.tickCount >= 30) && this.position().distanceToSqr(aimPoint) < 1.4D) {
                    this.explode();
                }
            }
        } else if (profile.usesWireGuidance() && owner instanceof LivingEntity livingOwner) {
            Vec3 desired = livingOwner.getViewVector(1.0F).normalize().scale(profile.maxSpeed());
            this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - profile.turnRate()).add(desired.scale(profile.turnRate())));
        }
    }

    /** Superb Warfare Javelin/Igla style: lofted top-attack, progressive turn, then dive. */
    private void steerJavelinStyle(VehicleMissileProfile profile, @Nullable Entity target, Vec3 aimPoint) {
        if (this.tickCount <= 3) {
            return;
        }
        double horizontalSqr = this.position().vectorTo(aimPoint).horizontalDistanceSqr();
        boolean close = horizontalSqr < 900.0D;
        Vec3 desiredPoint = aimPoint;
        if (this.topAttack) {
            if (!close) {
                double loft = Math.min(90.0D, 6.0D * this.tickCount);
                desiredPoint = new Vec3(aimPoint.x, aimPoint.y + loft, aimPoint.z);
            } else if (this.getY() < aimPoint.y) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8D));
                return;
            }
        } else {
            double dis = this.position().vectorTo(aimPoint).horizontalDistance();
            double height = dis > 30.0D ? 0.2D * (dis - 30.0D) : 0.0D;
            desiredPoint = aimPoint.add(0.0D, height, 0.0D);
        }

        Vec3 toVec = desiredPoint.subtract(this.position()).normalize();
        double turn = this.topAttack && close ? 0.55D : profile.turnRate();
        Vec3 desired = toVec.scale(profile.maxSpeed());
        this.setDeltaMovement(this.getDeltaMovement().scale(1.0D - turn).add(desired.scale(turn)));
        if (this.topAttack && close) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.15D).add(toVec.scale(profile.maxSpeed() * 0.95D)));
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.92D).add(toVec.scale(0.08D * profile.maxSpeed())));

        if (target != null && this.distanceToSqr(target) < 1.4D) {
            this.directHitEntityId = target.getId();
            if (target instanceof VehicleEntity) {
                this.directHitVehicleId = target.getId();
                this.setPos(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
            }
            this.explode();
        } else if (target == null && this.position().distanceToSqr(aimPoint) < 1.4D) {
            this.explode();
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
            this.directHitVehicleId = closest instanceof VehicleEntity ? closest.getId() : -1;
            this.directHitEntityId = closest.getId();
            this.explode();
            return true;
        }
        return false;
    }

    private boolean canImpact(Entity target, @Nullable Entity owner, @Nullable Entity ownerVehicle) {
        return target.isAlive()
                && target != owner
                && target != ownerVehicle
                && (ownerVehicle == null || target.getVehicle() != ownerVehicle)
                && (target instanceof LivingEntity || target instanceof VehicleEntity);
    }

    @Nullable
    private Entity currentTarget() {
        return this.targetId < 0 ? null : this.level().getEntity(this.targetId);
    }

    private void explode() {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            VehicleMissileProfile profile = this.profile();
            this.spawnMissileExplosionEffects(serverLevel);
            Entity owner = this.ownerId < 0 ? null : this.level().getEntity(this.ownerId);
            boolean directHitVehicle = this.directHitVehicleId >= 0;
            float explosionPower = directHitVehicle ? profile.explosionPower() * VEHICLE_DIRECT_HIT_BLOCK_DAMAGE_SCALE : profile.explosionPower();
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionPower, ExplosionInteraction.MOB);
            Entity ownerVehicle = owner == null ? null : owner.getVehicle();
            Vec3 explosionPos = this.position();
            for (VehicleEntity target : this.level().getEntitiesOfClass(VehicleEntity.class, this.getBoundingBox().inflate(profile.blastRadius()))) {
                if (target != ownerVehicle) {
                    float damage = target.getId() == this.directHitEntityId
                            ? profile.vehicleDamage() * (this.topAttack ? 1.25F : 1.0F)
                            : profile.vehicleDamage();
                    if (directHitVehicle && target.getId() != this.directHitVehicleId) {
                        double effectiveDistance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(explosionPos) * VEHICLE_DIRECT_HIT_FALLOFF_DISTANCE_SCALE;
                        if (effectiveDistance > profile.blastRadius()) {
                            continue;
                        }
                        damage *= 1.0F - (float) (effectiveDistance / profile.blastRadius());
                    }
                    if (damage > 0.5F) {
                        target.hurt(this.damageSources().explosion(this, owner), damage);
                    }
                }
            }
            for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(profile.blastRadius()))) {
                if (target != owner) {
                    float damage = target.getId() == this.directHitEntityId
                            ? profile.livingDamage() * (this.topAttack ? 1.25F : 1.0F)
                            : profile.livingDamage();
                    if (directHitVehicle) {
                        double effectiveDistance = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).distanceTo(explosionPos) * VEHICLE_DIRECT_HIT_FALLOFF_DISTANCE_SCALE;
                        if (effectiveDistance > profile.blastRadius()) {
                            continue;
                        }
                        damage *= 1.0F - (float) (effectiveDistance / profile.blastRadius());
                    }
                    if (damage > 0.5F) {
                        target.hurt(this.damageSources().explosion(this, owner), damage);
                    }
                }
            }
        }
        this.discard();
    }

    private void spawnMissileExplosionEffects(ServerLevel serverLevel) {
        sendLongDistanceParticles(serverLevel, ModParticleTypes.SMALL_EXPLOSION.get(), this.getX(), this.getY(), this.getZ(), 8, 0.7D, 0.7D, 0.7D, 0.08D);
        sendLongDistanceParticles(serverLevel, ModParticleTypes.SMOKE.get(), this.getX(), this.getY(), this.getZ(), 6, 0.8D, 0.8D, 0.8D, 0.02D);
    }

    private static <T extends ParticleOptions> void sendLongDistanceParticles(
            ServerLevel serverLevel,
            T particle,
            double x,
            double y,
            double z,
            int count,
            double xOffset,
            double yOffset,
            double zOffset,
            double speed
    ) {
        for (ServerPlayer player : serverLevel.players()) {
            serverLevel.sendParticles(player, particle, true, false, x, y, z, count, xOffset, yOffset, zOffset, speed);
        }
    }

    private VehicleMissileProfile profile() {
        return VehicleMissileProfile.get(this.weaponId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.ownerId = input.getIntOr("OwnerId", -1);
        this.targetId = input.getIntOr("TargetId", -1);
        this.weaponId = Identifier.parse(input.getStringOr("WeaponId", Reference.id("vehicle_bmp2_missile").toString()));
        if (input.getBooleanOr("HasTargetPos", false)) {
            this.targetPosition = new Vec3(
                    input.getDoubleOr("TargetX", 0.0D),
                    input.getDoubleOr("TargetY", 0.0D),
                    input.getDoubleOr("TargetZ", 0.0D)
            );
        }
        this.topAttack = input.getBooleanOr("TopAttack", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("OwnerId", this.ownerId);
        output.putInt("TargetId", this.targetId);
        output.putString("WeaponId", this.weaponId.toString());
        if (this.targetPosition != null) {
            output.putBoolean("HasTargetPos", true);
            output.putDouble("TargetX", this.targetPosition.x);
            output.putDouble("TargetY", this.targetPosition.y);
            output.putDouble("TargetZ", this.targetPosition.z);
        } else {
            output.putBoolean("HasTargetPos", false);
        }
        output.putBoolean("TopAttack", this.topAttack);
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        return false;
    }
}
