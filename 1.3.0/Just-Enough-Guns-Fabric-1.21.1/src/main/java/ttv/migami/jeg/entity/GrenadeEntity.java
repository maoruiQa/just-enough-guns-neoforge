package ttv.migami.jeg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;

public class GrenadeEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(
            GrenadeEntity.class,
            EntityDataSerializers.INT
    );
    private static final int DEFAULT_FUSE = 600;
    private static final float BOUNCE_DAMPING = 0.6F;
    private static final float SLIDE_DAMPING = 0.7F;
    private static final double LAUNCHED_GRAVITY = 0.015D;
    private static final double DAMAGE_RADIUS_MULTIPLIER = 2.0D;
    private static final float BALANCED_DAMAGE_FACTOR = 5.0F;
    private static final float EDGE_DAMAGE_FLOOR = 0.35F;
    private static final double FALLOFF_EXPONENT = 0.8D;
    private static final int OWNER_COLLISION_SAFE_TICKS = 8;

    @Override
    protected double getDefaultGravity() {
        return this.launched ? LAUNCHED_GRAVITY : super.getDefaultGravity();
    }

    private float explosionPower = 3.0F;
    private boolean launched;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("grenade")).get().getDefaultInstance());
    }

    public GrenadeEntity(Level level, LivingEntity owner, float explosionPower, int fuseTicks, boolean launched) {
        this(ModEntities.GRENADE.get(), level);
        this.setOwner(owner);
        this.entityData.set(DATA_FUSE, Math.max(5, fuseTicks));
        this.explosionPower = explosionPower;
        this.launched = launched;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    public void tick() {
        super.tick();

        // Add flame particle trail for visual effect
        if (this.level().isClientSide()) {
            Vec3 motion = this.getDeltaMovement();
            double speed = motion.length();
            if (speed > 0.1D) {
                // Spawn flame particles along the trajectory
                this.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.FLAME,
                    this.getX(),
                    this.getY() + 0.1D,
                    this.getZ(),
                    -motion.x * 0.1D,
                    -motion.y * 0.1D,
                    -motion.z * 0.1D
                );
                // Add smoke particles for better visibility
                if (this.random.nextInt(2) == 0) {
                    this.level().addParticle(
                        net.minecraft.core.particles.ParticleTypes.SMOKE,
                        this.getX(),
                        this.getY() + 0.1D,
                        this.getZ(),
                        -motion.x * 0.05D,
                        -motion.y * 0.05D,
                        -motion.z * 0.05D
                    );
                }
            }
        }

        if (!this.level().isClientSide()) {
            int fuse = this.entityData.get(DATA_FUSE) - 1;
            if (fuse <= 0) {
                explode();
            } else {
                this.entityData.set(DATA_FUSE, fuse);
            }
        }

        if (this.onGround()) {
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
        explode();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hitEntity = result.getEntity();
        if (hitEntity == this.getOwner() && this.tickCount <= OWNER_COLLISION_SAFE_TICKS) {
            return;
        }
        explode();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", this.entityData.get(DATA_FUSE));
        output.putFloat("Power", this.explosionPower);
        output.putBoolean("Launched", this.launched);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_FUSE, Mth.clamp(input.getIntOr("Fuse", DEFAULT_FUSE), 5, DEFAULT_FUSE));
        this.explosionPower = input.getFloatOr("Power", this.explosionPower);
        this.launched = input.getBooleanOr("Launched", this.launched);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("grenade")).get();
    }

    public void initialisePosition(Vec3 position) {
        this.setPos(position);
    }

    public void setExplosionPower(float power) {
        this.explosionPower = power;
    }

    public void setLaunched(boolean launched) {
        this.launched = launched;
    }

    private void explode() {
        if (!this.level().isClientSide()) {
            ExplosionInteraction interaction = this.launched ? ExplosionInteraction.TNT : ExplosionInteraction.MOB;
            float visualPower = Math.max(1.2F, this.explosionPower * 0.5F);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), visualPower, interaction);
            applyBalancedBlastDamage();
            igniteNearby();
        }
        this.discard();
    }


    private void applyBalancedBlastDamage() {
        if (this.level().isClientSide()) {
            return;
        }

        double radius = Math.max(2.6D, this.explosionPower * DAMAGE_RADIUS_MULTIPLIER);
        float baseDamage = this.explosionPower * BALANCED_DAMAGE_FACTOR;
        Entity owner = this.getOwner();
        AABB area = this.getBoundingBox().inflate(radius);

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive()) {
                continue;
            }

            double distance = target.distanceTo(this);
            if (distance > radius) {
                continue;
            }

            double t = 1.0D - (distance / radius);
            double curve = Math.pow(Math.max(0.0D, t), FALLOFF_EXPONENT);
            float scale = (float) (EDGE_DAMAGE_FLOOR + (1.0F - EDGE_DAMAGE_FLOOR) * curve);
            float damage = baseDamage * scale;

            if (owner != null && target == owner) {
                damage *= 0.65F;
            }

            if (damage > 0.5F) {
                target.hurt(this.damageSources().explosion(this, owner), damage);
            }
        }
    }

    private void igniteNearby() {
        if (this.launched) {
            return;
        }
        Level level = this.level();
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }
            if (level.random.nextBoolean() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
