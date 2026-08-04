package ttv.migami.jeg.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.particle.CustomSmokeOption;
import ttv.migami.jeg.util.SmokeCloudTracker;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;

/** Superb Warfare M18 smoke grenade with immediate lock denial when smoking. */
public final class SmokeGrenadeEntity extends TimedThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> DATA_SMOKING =
            SynchedEntityData.defineId(SmokeGrenadeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int SMOKE_DECOY_COUNT = 8;
    private static final int DEFAULT_SMOKE_FUSE = 80;
    private static final CustomSmokeOption WHITE_SMOKE = new CustomSmokeOption(1.0F, 1.0F, 1.0F);

    private int smokeFuse = DEFAULT_SMOKE_FUSE;
    private boolean released;

    public SmokeGrenadeEntity(net.minecraft.world.entity.EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
        this.setItem(ModItems.AMMO.get(Reference.id("smoke_grenade")).get().getDefaultInstance());
    }

    public SmokeGrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        this(ModEntities.SMOKE_GRENADE.get(), level);
        this.setOwner(owner);
        this.smokeFuse = Math.max(40, fuseTicks <= 0 ? DEFAULT_SMOKE_FUSE : fuseTicks);
        this.setFuse(1200);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SMOKING, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AMMO.get(Reference.id("smoke_grenade")).get();
    }

    @Override
    protected boolean usesStandardFuse() {
        return false;
    }

    @Override
    protected double getThrownGravity() {
        return 0.07D;
    }

    public boolean isActivelySmoking() {
        return this.entityData.get(DATA_SMOKING) || this.smokeFuse <= 0 || this.released;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            if (this.isActivelySmoking()) {
                SmokeCloudTracker.report(this.level(), this.getX(), this.getY(), this.getZ());
            }
            return;
        }

        this.smokeFuse--;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.xo, this.yo, this.zo, 1, 0.0D, 0.0D, 0.0D, 0.01D);
        }

        if (this.smokeFuse == 0) {
            this.entityData.set(DATA_SMOKING, true);
            SoundEvent sound = resolveSound("entity.smoke_grenade.release", SoundEvents.FIRE_EXTINGUISH);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, 2.0F, 1.0F);
            if (!this.released) {
                this.released = true;
                this.releaseSmokeDecoys();
            }
        }

        if (this.smokeFuse <= 0) {
            this.entityData.set(DATA_SMOKING, true);
            SmokeCloudTracker.report(this.level(), this.getX(), this.getY(), this.getZ());
            if (this.tickCount % 2 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        WHITE_SMOKE,
                        this.getX(),
                        this.getY() + this.getBbHeight(),
                        this.getZ(),
                        8,
                        0.075D,
                        0.01D,
                        0.075D,
                        0.08D
                );
            }
        }

        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void spawnFlightParticles() {
    }

    private void releaseSmokeDecoys() {
        Vec3 origin = new Vec3(this.getX(), this.getY() + this.getBbHeight(), this.getZ());
        this.level().addFreshEntity(VehicleDecoyEntity.smoke(
                this.level(), this, origin, new Vec3(0.0D, 0.05D, 0.0D), 0.0F, 0.0F, false));
        Vec3 base = new Vec3(1.0D, 0.05D, 0.0D);
        for (int i = 0; i < SMOKE_DECOY_COUNT; i++) {
            float yaw = i * (360.0F / SMOKE_DECOY_COUNT) * Mth.DEG_TO_RAD;
            this.level().addFreshEntity(VehicleDecoyEntity.smoke(
                    this.level(), this, origin, base.yRot(yaw), 1.5F, 5.0F, false));
        }
        SmokeCloudTracker.report(this.level(), origin.x, origin.y, origin.z);
    }

    @Override
    protected void explode() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("SmokeFuse", this.smokeFuse);
        output.putBoolean("SmokeReleased", this.released);
        output.putBoolean("Smoking", this.entityData.get(DATA_SMOKING));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        this.smokeFuse = input.contains("SmokeFuse") ? input.getInt("SmokeFuse") : DEFAULT_SMOKE_FUSE;
        this.released = input.contains("SmokeReleased") && input.getBoolean("SmokeReleased");
        this.entityData.set(DATA_SMOKING, input.contains("Smoking")
                ? input.getBoolean("Smoking")
                : (this.smokeFuse <= 0 || this.released));
    }

    private static SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }
}
