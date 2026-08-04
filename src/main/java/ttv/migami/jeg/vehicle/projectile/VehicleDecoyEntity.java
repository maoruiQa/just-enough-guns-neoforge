package ttv.migami.jeg.vehicle.projectile;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.particle.CustomSmokeOption;
import ttv.migami.jeg.util.SmokeCloudTracker;
import ttv.migami.jeg.util.SmokeUtil;

/** Smoke display matches Superb Warfare SmokeDecoyEntity (one-shot ignite only). */
public final class VehicleDecoyEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_SMOKE = SynchedEntityData.defineId(VehicleDecoyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int FLARE_LIFE_TICKS = 200;
    private static final int SMOKE_LIFE_TICKS = 400;
    private static final int SMOKE_IGNITE_TICKS = 4;
    private static final CustomSmokeOption WHITE_SMOKE = new CustomSmokeOption(1.0F, 1.0F, 1.0F);

    private boolean releaseSmoke = true;

    public VehicleDecoyEntity(EntityType<? extends VehicleDecoyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    private VehicleDecoyEntity(Level level, Vec3 position, Vec3 velocity, boolean smoke, boolean releaseSmoke) {
        this(ModEntities.VEHICLE_DECOY.get(), level);
        this.entityData.set(DATA_SMOKE, smoke);
        this.releaseSmoke = releaseSmoke;
        this.setPos(position);
        this.setDeltaMovement(velocity);
        if (smoke) {
            this.refreshDimensions();
            this.reapplySmokeBounds();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.isSmokeDecoy() ? EntityDimensions.scalable(4.5F, 4.5F) : super.getDimensions(pose);
    }

    public static VehicleDecoyEntity flare(Level level, Entity shooter, Vec3 position, Vec3 direction, float velocity, float inaccuracy) {
        VehicleDecoyEntity decoy = new VehicleDecoyEntity(level, position, Vec3.ZERO, false, false);
        decoy.shootFrom(shooter, direction, velocity, inaccuracy);
        return decoy;
    }

    public static VehicleDecoyEntity smoke(Level level, Entity shooter, Vec3 position, Vec3 direction, float velocity, float inaccuracy) {
        return smoke(level, shooter, position, direction, velocity, inaccuracy, true);
    }

    public static VehicleDecoyEntity smoke(
            Level level,
            Entity shooter,
            Vec3 position,
            Vec3 direction,
            float velocity,
            float inaccuracy,
            boolean releaseSmoke
    ) {
        VehicleDecoyEntity decoy = new VehicleDecoyEntity(level, position, Vec3.ZERO, true, releaseSmoke);
        decoy.shootFrom(shooter, direction, velocity, inaccuracy);
        return decoy;
    }

    public static Optional<VehicleDecoyEntity> findNearest(Level level, Vec3 position, double range) {
        AABB area = new AABB(position, position).inflate(range);
        return level.getEntitiesOfClass(VehicleDecoyEntity.class, area).stream()
                .min(Comparator.comparingDouble(decoy -> decoy.distanceToSqr(position)));
    }

    public static Optional<VehicleDecoyEntity> findNearestFlare(Level level, Vec3 position, double range) {
        AABB area = new AABB(position, position).inflate(range);
        return level.getEntitiesOfClass(VehicleDecoyEntity.class, area, decoy -> !decoy.isSmokeDecoy()).stream()
                .min(Comparator.comparingDouble(decoy -> decoy.distanceToSqr(position)));
    }

    public static boolean isSmokeBlockingTarget(Entity target) {
        return SmokeUtil.isSmokeBlockingTarget(target);
    }

    public boolean isSmokeDecoy() {
        return this.entityData.get(DATA_SMOKE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SMOKE, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isSmokeDecoy()) {
            this.tickSmoke();
            return;
        }
        this.tickFlare();
    }

    private void tickFlare() {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.02D, 0.0D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.level().isClientSide) {
            this.level().addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, this.xo, this.yo, this.zo, 0.0D, 0.0D, 0.0D);
            this.level().addAlwaysVisibleParticle(ParticleTypes.CLOUD, true, this.xo, this.yo, this.zo, 0.0D, 0.0D, 0.0D);
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0D, 0.01D, 0.0D);
        }
        if (this.tickCount > FLARE_LIFE_TICKS || this.isInWater() || this.onGround()) {
            this.discard();
        }
    }

    private void tickSmoke() {
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.reapplySmokeBounds();

        if (this.tickCount == SMOKE_IGNITE_TICKS) {
            if (this.releaseSmoke && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(WHITE_SMOKE, this.xo, this.yo, this.zo, 50, 0.0D, 0.0D, 0.0D, 0.07D);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.xo, this.yo, this.zo, 10, 1.0D, 1.0D, 1.0D, 0.1D);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.xo, this.yo, this.zo, 30, 0.0D, 0.0D, 0.0D, 0.2D);
                SoundEvent sound = resolveSound("entity.smoke_grenade.smoke_fire", SoundEvents.FIRE_EXTINGUISH);
                this.level().playSound(null, this, sound, this.getSoundSource(), 2.0F, this.random.nextFloat() * 0.05F + 1.0F);
            }
            this.setDeltaMovement(Vec3.ZERO);
        }

        SmokeCloudTracker.report(this.level(), this.getX(), this.getY(), this.getZ());

        if (!this.level().isClientSide && this.tickCount > SMOKE_IGNITE_TICKS && this.tickCount % 10 == 0) {
            SmokeUtil.applySmokedNearby(this);
        }

        if (this.tickCount > SMOKE_LIFE_TICKS) {
            this.discard();
        }
    }

    private void reapplySmokeBounds() {
        if (!this.isSmokeDecoy()) {
            return;
        }
        double half = 2.25D;
        double height = 4.5D;
        this.setBoundingBox(new AABB(
                this.getX() - half,
                this.getY(),
                this.getZ() - half,
                this.getX() + half,
                this.getY() + height,
                this.getZ() + half
        ));
    }

    private void shootFrom(Entity shooter, Vec3 direction, float velocity, float inaccuracy) {
        Vec3 randomOffset = new Vec3(
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy),
                this.random.triangle(0.0D, 0.0172275D * inaccuracy)
        );
        Vec3 shot = direction.normalize().add(randomOffset).normalize().scale(velocity);
        this.setDeltaMovement(shooter.getDeltaMovement().scale(0.75D).add(shot));
        double horizontal = shot.horizontalDistance();
        this.setYRot((float) (Mth.atan2(shot.x, shot.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(shot.y, horizontal) * Mth.RAD_TO_DEG));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_SMOKE, tag.getBoolean("Smoke"));
        this.releaseSmoke = !tag.contains("ReleaseSmoke") || tag.getBoolean("ReleaseSmoke");
        if (this.isSmokeDecoy()) {
            this.refreshDimensions();
            this.reapplySmokeBounds();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Smoke", this.isSmokeDecoy());
        tag.putBoolean("ReleaseSmoke", this.releaseSmoke);
    }

    private static SoundEvent resolveSound(String path, SoundEvent fallback) {
        var holder = ModSounds.ALL.get(Reference.id(path));
        return holder != null ? holder.get() : fallback;
    }
}
