package ttv.migami.jeg.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lightweight projectile for bow-style guns. Keeps the implementation simple so the
 * 1.21 port can compile without the legacy projectile framework.
 */
public class ArrowProjectileEntity extends Projectile {
    private static final double GRAVITY = 0.05D;
    private static final double FRICTION = 0.99D;
    private static final int DEFAULT_LIFETIME = 80;

    private int lifetime = DEFAULT_LIFETIME;
    private boolean flaming;
    private boolean charged;

    public ArrowProjectileEntity(EntityType<? extends ArrowProjectileEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public ArrowProjectileEntity(
        EntityType<? extends ArrowProjectileEntity> type,
        Level level,
        LivingEntity shooter,
        Vec3 velocity,
        boolean flaming,
        boolean charged
    ) {
        this(type, level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
        this.setDeltaMovement(velocity);
        this.flaming = flaming;
        this.charged = charged;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No tracked properties required for the fallback arrow.
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            if (this.tickCount > this.lifetime) {
                this.discard();
                return;
            }
        } else {
            spawnVisuals();
        }

        Vec3 velocity = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);

        velocity = this.getDeltaMovement();
        if (!this.isNoGravity()) {
            velocity = velocity.add(0.0D, -GRAVITY, 0.0D);
        }
        this.setDeltaMovement(velocity.scale(FRICTION));
    }

    private void spawnVisuals() {
        Vec3 reverse = this.getDeltaMovement().scale(-0.25D);
        double x = this.getX() + reverse.x;
        double y = this.getY() + reverse.y;
        double z = this.getZ() + reverse.z;

        if (this.flaming) {
            this.level().addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        if (this.charged) {
            this.level().addParticle(ParticleTypes.CRIT, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        if (this.isUnderWater()) {
            this.level().addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.flaming) {
            Entity entity = result.getEntity();
            if (!entity.fireImmune()) {
                int current = entity.getRemainingFireTicks();
                entity.setRemainingFireTicks(Math.max(current, 100));
            }
        }
        if (!this.level().isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            if (this.flaming) {
                BlockPos firePos = result.getBlockPos().relative(result.getDirection());
                if (BaseFireBlock.canBePlacedAt(this.level(), firePos, result.getDirection())) {
                    BlockState state = BaseFireBlock.getState(this.level(), firePos);
                    this.level().setBlock(firePos, state, 11);
                }
            }
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Lifetime", this.lifetime);
        output.putBoolean("Flaming", this.flaming);
        output.putBoolean("Charged", this.charged);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lifetime = input.getIntOr("Lifetime", this.lifetime);
        this.flaming = input.getBooleanOr("Flaming", this.flaming);
        this.charged = input.getBooleanOr("Charged", this.charged);
    }

    public void setLifetime(int ticks) {
        this.lifetime = Math.max(1, ticks);
    }

    public void setFlaming(boolean flaming) {
        this.flaming = flaming;
    }

    public void setCharged(boolean charged) {
        this.charged = charged;
    }

    public boolean isFlaming() {
        return this.flaming;
    }

    public boolean isCharged() {
        return this.charged;
    }
}
