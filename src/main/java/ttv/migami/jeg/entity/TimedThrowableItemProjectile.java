package ttv.migami.jeg.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
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
        super.tick();

        if (this.level().isClientSide()) {
            this.spawnFlightParticles();
        } else {
            int fuse = this.getFuse() - 1;
            if (fuse <= 0) {
                this.explodeNow();
                return;
            }
            this.setFuse(fuse);
        }

        if (this.shouldBounce() && this.onGround()) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x * SLIDE_DAMPING, motion.y * -BOUNCE_DAMPING, motion.z * SLIDE_DAMPING);
            if (motion.lengthSqr() < 0.03D) {
                this.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.shouldDetonateOnImpact()) {
            this.explodeNow();
        }
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
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", this.getFuse());
        output.putBoolean("Launched", this.launched);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setFuse(Mth.clamp(input.getIntOr("Fuse", DEFAULT_FUSE), 1, 1200));
        this.launched = input.getBooleanOr("Launched", false);
    }

    public final int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public final void setFuse(int fuseTicks) {
        this.entityData.set(DATA_FUSE, Math.max(1, fuseTicks));
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
