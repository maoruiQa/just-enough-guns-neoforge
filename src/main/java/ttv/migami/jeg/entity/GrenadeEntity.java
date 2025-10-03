package ttv.migami.jeg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
    private static final int DEFAULT_FUSE = 60;
    private static final float BOUNCE_DAMPING = 0.6F;
    private static final float SLIDE_DAMPING = 0.7F;

    private float explosionPower = 2.4F;
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

        if (!this.level().isClientSide) {
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
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * SLIDE_DAMPING, -motion.y * BOUNCE_DAMPING, motion.z * SLIDE_DAMPING);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            DamageSource source = this.damageSources().explosion(this, this.getOwner());
            result.getEntity().hurt(source, 2.0F);
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
        this.entityData.set(DATA_FUSE, Mth.clamp(input.getIntOr("Fuse", DEFAULT_FUSE), 5, 200));
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
        if (!this.level().isClientSide) {
            ExplosionInteraction interaction = this.launched ? ExplosionInteraction.TNT : ExplosionInteraction.MOB;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionPower, interaction);
            igniteNearby();
        }
        this.discard();
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
