package ttv.migami.jeg.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class TimedThrowableItemProjectile extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(
            TimedThrowableItemProjectile.class,
            EntityDataSerializers.INT
    );
    private static final int DEFAULT_FUSE = 60;
    private static final float BOUNCE_DAMPING = 0.6F;
    private static final float SLIDE_DAMPING = 0.7F;
    private static final int OWNER_COLLISION_SAFE_TICKS = 8;
    private static final double HIT_POSITION_EPSILON = 0.05D;
    private static final double MIN_BOUNCE_SPEED_SQR = 0.0009D;
    private static final double ENTITY_BOUNCE_DAMPING = 0.35D;

    private boolean launched;

    protected TimedThrowableItemProjectile(EntityType<? extends TimedThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    protected TimedThrowableItemProjectile(
            EntityType<? extends TimedThrowableItemProjectile> type,
            Level level,
            LivingEntity owner,
            int fuseTicks
    ) {
        this(type, level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    protected double getDefaultGravity() {
        return this.launched ? this.getLaunchedGravity() : this.getThrownGravity();
    }

    @Override
    public void tick() {
        this.baseTick();

        if (!this.isAlive()) {
            return;
        }

        if (this.level().isClientSide()) {
            this.spawnFlightParticles();
        } else if (this.usesStandardFuse()) {
            int fuse = this.getFuse() - 1;
            if (fuse <= 0) {
                this.explodeNow();
                return;
            }
            this.setFuse(fuse);
        }

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-7D) {
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.onHit(hitResult);
                if (!this.isAlive()) {
                    return;
                }
                return;
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.checkInsideBlocks();

        Vec3 updatedMotion = this.getDeltaMovement();
        double inertia = this.isInWater() ? 0.8D : 0.99D;
        if (!this.isNoGravity()) {
            updatedMotion = updatedMotion.add(0.0D, -this.getDefaultGravity(), 0.0D);
        }
        this.setDeltaMovement(updatedMotion.scale(inertia));

        if (this.shouldBounce() && this.onGround()) {
            Vec3 groundMotion = this.getDeltaMovement();
            this.setDeltaMovement(groundMotion.x * SLIDE_DAMPING, groundMotion.y * -BOUNCE_DAMPING, groundMotion.z * SLIDE_DAMPING);
            if (groundMotion.lengthSqr() < 0.03D) {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity)) {
            return false;
        }
        return entity != this.getOwner() || this.tickCount > OWNER_COLLISION_SAFE_TICKS;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.shouldDetonateOnImpact()) {
            this.explodeNow();
            return;
        }
        this.bounceOffBlock(result);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hitEntity = result.getEntity();
        if (hitEntity == this.getOwner() && this.tickCount <= OWNER_COLLISION_SAFE_TICKS) {
            return;
        }
        if (this.shouldDetonateOnImpact()) {
            this.explodeNow();
            return;
        }
        this.bounceOffEntity(result);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", this.getFuse());
        output.putBoolean("Launched", this.launched);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        this.setFuse(Mth.clamp(input.contains("Fuse") ? input.getInt("Fuse") : DEFAULT_FUSE, 1, 1200));
        this.launched = input.contains("Launched") && input.getBoolean("Launched");
    }

    public final int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public final void setFuse(int fuseTicks) {
        this.entityData.set(DATA_FUSE, fuseTicks);
    }

    protected boolean usesStandardFuse() {
        return true;
    }

    public final void initialisePosition(Vec3 position) {
        this.setPos(position);
    }

    public final void setLaunched(boolean launched) {
        this.launched = launched;
    }

    public final boolean isLaunched() {
        return this.launched;
    }

    public final void explodeNow() {
        if (this.level().isClientSide() || !this.isAlive()) {
            return;
        }
        this.explode();
        this.discard();
    }

    private void bounceOffBlock(BlockHitResult result) {
        Direction direction = result.getDirection();
        if (direction == Direction.UP) {
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < MIN_BOUNCE_SPEED_SQR) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 hitNormal = Vec3.atLowerCornerOf(direction.getNormal());
        Vec3 correctedPosition = result.getLocation().add(hitNormal.scale(HIT_POSITION_EPSILON));
        this.setPos(correctedPosition);

        Vec3 bouncedMotion = switch (direction.getAxis()) {
            case X -> new Vec3(-motion.x * BOUNCE_DAMPING, motion.y * SLIDE_DAMPING, motion.z * SLIDE_DAMPING);
            case Y -> new Vec3(motion.x * SLIDE_DAMPING, -motion.y * BOUNCE_DAMPING, motion.z * SLIDE_DAMPING);
            case Z -> new Vec3(motion.x * SLIDE_DAMPING, motion.y * SLIDE_DAMPING, -motion.z * BOUNCE_DAMPING);
        };

        this.setDeltaMovement(bouncedMotion.lengthSqr() < MIN_BOUNCE_SPEED_SQR ? Vec3.ZERO : bouncedMotion);
        this.hasImpulse = true;
    }

    private void bounceOffEntity(EntityHitResult result) {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() < MIN_BOUNCE_SPEED_SQR) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 impactDirection = motion.normalize();
        Vec3 correctedPosition = result.getLocation().subtract(impactDirection.scale(HIT_POSITION_EPSILON));
        this.setPos(correctedPosition);

        Vec3 bouncedMotion = motion.scale(-ENTITY_BOUNCE_DAMPING);
        this.setDeltaMovement(bouncedMotion.lengthSqr() < MIN_BOUNCE_SPEED_SQR ? Vec3.ZERO : bouncedMotion);
        this.hasImpulse = true;
    }

    protected boolean shouldBounce() {
        return true;
    }

    protected boolean shouldDetonateOnImpact() {
        return false;
    }

    protected double getThrownGravity() {
        return 0.05D;
    }

    protected double getLaunchedGravity() {
        return 0.015D;
    }

    protected void spawnFlightParticles() {
    }

    protected abstract void explode();
}
