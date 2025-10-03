package ttv.migami.jeg.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModEntities;

public class BulletEntity extends Projectile {
    private static final EntityDataAccessor<String> DATA_GUN = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRAIL_COLOR = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_TRAIL_LENGTH = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(BulletEntity.class, EntityDataSerializers.FLOAT);
    private static final ResourceLocation FLAMETHROWER_ID = Reference.id("flamethrower");
    private static final ResourceLocation ROCKET_LAUNCHER_ID = Reference.id("rocket_launcher");
    private static final ResourceLocation HYPERSONIC_ID = Reference.id("hypersonic_cannon");
    private static final ResourceLocation TYPHOONEE_ID = Reference.id("typhoonee");

    private int ticksLived;

    public BulletEntity(EntityType<? extends BulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public BulletEntity(Level level, LivingEntity shooter, GunStats stats, Vec3 velocity) {
        this(ModEntities.BULLET.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.entityData.set(DATA_GUN, stats.id().toString());
        this.entityData.set(DATA_DAMAGE, stats.damage());
        this.entityData.set(DATA_LIFE, stats.projectileLife());
        this.entityData.set(DATA_TRAIL_COLOR, stats.trailColor());
        this.entityData.set(DATA_TRAIL_LENGTH, stats.clampedTrailLength());
        this.entityData.set(DATA_SIZE, stats.clampedProjectileSize());
        this.setVelocityAndRotation(velocity);
        if (!stats.id().equals(FLAMETHROWER_ID)) {
            this.setNoGravity(!stats.gravity());
        }
        this.refreshDimensions();
        this.setOldPosAndRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_GUN, Reference.id("assault_rifle").toString());
        builder.define(DATA_DAMAGE, 4.0F);
        builder.define(DATA_LIFE, 40);
        builder.define(DATA_TRAIL_COLOR, 0xFFFFFFFF);
        builder.define(DATA_TRAIL_LENGTH, 1.0F);
        builder.define(DATA_SIZE, 0.05F);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = this.getDeltaMovement();
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }

        if (this.isAlive()) {
            // Spawn particles along bullet trail BEFORE moving (so particles spawn along the path)
            if (this.level().isClientSide) {
                ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));
                if (gunId.equals(FLAMETHROWER_ID)) {
                    spawnFlameParticles();
                } else {
                    // Spawn particles along the entire movement path
                    spawnBulletTrailParticlesAlongPath(motion);
                }
            }

            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            if (!this.isNoGravity()) {
                // Enhanced gravity for flamethrower to simulate realistic fire stream drop
                ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));
                double gravityAccel = gunId.equals(FLAMETHROWER_ID) ? 0.25 : 0.05;
                this.setDeltaMovement(motion.x, motion.y - gravityAccel, motion.z);
            }
        }

        if (++this.ticksLived > this.entityData.get(DATA_LIFE)) {
            // For flamethrower, create fire at final position before discarding
            ResourceLocation gunId = ResourceLocation.parse(this.entityData.get(DATA_GUN));
            if (gunId.equals(FLAMETHROWER_ID) && !this.level().isClientSide) {
                igniteArea(this.blockPosition());
            }
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        Entity owner = this.getOwner();
        if (handleSpecialImpact(result)) {
            this.discard();
            return;
        }
        if (!this.level().isClientSide) {
            float damage = this.entityData.get(DATA_DAMAGE);
            DamageSource source;
            if (owner instanceof LivingEntity livingOwner) {
                source = this.damageSources().mobProjectile(this, livingOwner);
            } else {
                source = this.damageSources().thrown(this, owner);
            }

            if (entity instanceof LivingEntity living) {
                living.hurtServer((ServerLevel)this.level(), source, damage);
            } else {
                entity.hurt(source, damage);
            }
        }

        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (handleSpecialImpact(result)) {
            this.discard();
            return;
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putString("GunId", this.entityData.get(DATA_GUN));
        output.putFloat("Damage", this.entityData.get(DATA_DAMAGE));
        output.putInt("Life", this.entityData.get(DATA_LIFE));
        output.putInt("TrailColor", this.entityData.get(DATA_TRAIL_COLOR));
        output.putFloat("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH));
        output.putFloat("ProjectileSize", this.entityData.get(DATA_SIZE));
        output.putInt("Ticks", this.ticksLived);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.entityData.set(DATA_GUN, input.getStringOr("GunId", Reference.id("assault_rifle").toString()));
        this.entityData.set(DATA_DAMAGE, input.getFloatOr("Damage", this.entityData.get(DATA_DAMAGE)));
        this.entityData.set(DATA_LIFE, input.getIntOr("Life", this.entityData.get(DATA_LIFE)));
        this.entityData.set(DATA_TRAIL_COLOR, input.getIntOr("TrailColor", this.entityData.get(DATA_TRAIL_COLOR)));
        this.entityData.set(DATA_TRAIL_LENGTH, input.getFloatOr("TrailLength", this.entityData.get(DATA_TRAIL_LENGTH)));
        this.entityData.set(DATA_SIZE, input.getFloatOr("ProjectileSize", this.entityData.get(DATA_SIZE)));
        this.ticksLived = input.getIntOr("Ticks", this.ticksLived);
        this.refreshDimensions();
        this.setVelocityAndRotation(this.getDeltaMovement());
    }

    public GunStats getGunStats() {
        ResourceLocation id = ResourceLocation.parse(this.entityData.get(DATA_GUN));
        GunStats stats = GunDefinitions.ALL.get(id);
        if (stats == null) {
            return new GunStats(id, null, "jeg:mag_fed", 1, 20, 0, 10, this.entityData.get(DATA_DAMAGE), 4F, this.entityData.get(DATA_LIFE), false, 0F, 1F, 1, null, null, null, null, null, null, null, null, this.entityData.get(DATA_SIZE), this.entityData.get(DATA_TRAIL_COLOR), this.entityData.get(DATA_TRAIL_LENGTH));
        }
        return stats;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public int getTrailColor() {
        return this.entityData.get(DATA_TRAIL_COLOR);
    }

    public float getTrailLengthMultiplier() {
        return this.entityData.get(DATA_TRAIL_LENGTH);
    }

    public float getProjectileSize() {
        return this.entityData.get(DATA_SIZE);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float diameter = Mth.clamp(this.getProjectileSize(), 0.05F, 1.0F);
        return EntityDimensions.scalable(diameter, diameter).withEyeHeight(0.0F);
    }

    public void initialisePosition(Vec3 position) {
        this.setPos(position);
        this.setOldPosAndRot();
    }

    private void setVelocityAndRotation(Vec3 velocity) {
        this.setDeltaMovement(velocity);
        double length = velocity.length();
        if (length <= 1.0E-5D) {
            return;
        }

        Vec3 normalized = velocity.scale(1.0D / length);
        float yaw = (float)(Mth.atan2(normalized.x, normalized.z) * (180F / Math.PI));
        float pitch = (float)(Mth.atan2(normalized.y, Math.sqrt(normalized.x * normalized.x + normalized.z * normalized.z)) * (180F / Math.PI));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    private boolean handleSpecialImpact(HitResult result) {
        GunStats stats = getGunStats();
        ResourceLocation id = stats.id();

        if (id.equals(FLAMETHROWER_ID)) {
            if (!this.level().isClientSide) {
                if (result instanceof EntityHitResult entityHit && entityHit.getEntity() != null) {
                    Entity hitEntity = entityHit.getEntity();
                    if (hitEntity instanceof LivingEntity living) {
                        living.igniteForSeconds(6);
                    } else {
                        hitEntity.igniteForSeconds(6);
                    }
                }
                igniteArea(BlockPos.containing(result.getLocation()));
            }
            return true;
        }

        if (id.equals(ROCKET_LAUNCHER_ID) || id.equals(HYPERSONIC_ID) || id.equals(TYPHOONEE_ID)) {
            if (!this.level().isClientSide) {
                float power = 2.5F;
                if (id.equals(HYPERSONIC_ID)) {
                    power = 3.5F;
                } else if (id.equals(TYPHOONEE_ID)) {
                    power = 3.1F;
                }
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, ExplosionInteraction.MOB);
            }
            return true;
        }

        return false;
    }

    private void igniteArea(BlockPos center) {
        Level level = this.level();
        if (level.isClientSide) {
            return;
        }

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                continue;
            }
            level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }

    private void spawnFlameParticles() {
        Level level = this.level();
        if (!level.isClientSide) {
            return;
        }

        // Spawn flame and smoke particles along the flamethrower stream
        for (int i = 0; i < 3; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            level.addParticle(ParticleTypes.FLAME,
                this.getX() + offsetX,
                this.getY() + offsetY,
                this.getZ() + offsetZ,
                0, 0.02, 0);
        }

        // Add occasional smoke
        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0.01, 0);
        }

        // Add large smoke particles less frequently
        if (level.random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE,
                this.getX(),
                this.getY(),
                this.getZ(),
                0, 0.015, 0);
        }
    }

    private void spawnBulletTrailParticlesAlongPath(Vec3 motion) {
        Level level = this.level();
        if (!level.isClientSide) {
            return;
        }

        // Only spawn 1 smoke particle at current position per tick
        // This creates a thin continuous trail as the bullet moves
        // The BulletRenderer handles the visual trail line
        level.addParticle(ParticleTypes.SMOKE,
            this.getX(),
            this.getY(),
            this.getZ(),
            0, 0, 0);
    }
}
