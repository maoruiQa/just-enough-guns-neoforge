package ttv.migami.jeg.vehicle.projectile;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModEntities;

public final class VehicleDecoyEntity extends Entity {
    private static final EntityDataAccessor<Boolean> DATA_SMOKE = SynchedEntityData.defineId(VehicleDecoyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int FLARE_LIFE_TICKS = 200;
    private static final int SMOKE_LIFE_TICKS = 400;
    private static final int SMOKE_IGNITE_TICKS = 4;
    private static final double SMOKE_BLOCK_RANGE = 12.0D;
    private static final double SMOKE_THICKNESS_MULTIPLIER = 1.5D;

    public VehicleDecoyEntity(EntityType<? extends VehicleDecoyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    private VehicleDecoyEntity(Level level, Vec3 position, Vec3 velocity, boolean smoke) {
        this(ModEntities.VEHICLE_DECOY.get(), level);
        this.entityData.set(DATA_SMOKE, smoke);
        this.setPos(position);
        this.setDeltaMovement(velocity);
    }

    public static VehicleDecoyEntity flare(Level level, Entity shooter, Vec3 position, Vec3 direction, float velocity, float inaccuracy) {
        VehicleDecoyEntity decoy = new VehicleDecoyEntity(level, position, Vec3.ZERO, false);
        decoy.shootFrom(shooter, direction, velocity, inaccuracy);
        return decoy;
    }

    public static VehicleDecoyEntity smoke(Level level, Entity shooter, Vec3 position, Vec3 direction, float velocity, float inaccuracy) {
        VehicleDecoyEntity decoy = new VehicleDecoyEntity(level, position, Vec3.ZERO, true);
        decoy.shootFrom(shooter, direction, velocity, inaccuracy);
        return decoy;
    }

    public static Optional<VehicleDecoyEntity> findNearest(Level level, Vec3 position, double range) {
        AABB area = new AABB(position, position).inflate(range);
        return level.getEntitiesOfClass(VehicleDecoyEntity.class, area).stream()
                .min(Comparator.comparingDouble(decoy -> decoy.distanceToSqr(position)));
    }

    public static boolean isSmokeBlockingTarget(Entity target) {
        AABB area = target.getBoundingBox().inflate(SMOKE_BLOCK_RANGE);
        return !target.level().getEntitiesOfClass(VehicleDecoyEntity.class, area, VehicleDecoyEntity::isSmokeDecoy).isEmpty();
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
        if (this.tickCount == SMOKE_IGNITE_TICKS) {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY(), this.getZ(), 240, 6.0D, 3.2D, 6.0D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 30, 0.25D, 0.25D, 0.25D, 0.12D);
            }
        }
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel && this.tickCount > SMOKE_IGNITE_TICKS && this.tickCount % 5 == 0) {
            double radius = this.smokeRadius();
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 72, radius, 2.8D, radius, 0.015D);
        }
        if (this.level().isClientSide) {
            for (int i = 0; i < 10; i++) {
                double radius = this.smokeRadius();
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * radius * 2.0D;
                double y = this.getY() + this.random.nextDouble() * 5.6D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * radius * 2.0D;
                this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, this.random.nextGaussian() * 0.01D, 0.015D, this.random.nextGaussian() * 0.01D);
            }
        }
        if (this.tickCount > SMOKE_LIFE_TICKS) {
            this.discard();
        }
    }

    private double smokeRadius() {
        return Mth.clamp(1.6D + (this.tickCount - SMOKE_IGNITE_TICKS) / 15.0D, 1.6D, 7.0D) * SMOKE_THICKNESS_MULTIPLIER;
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Smoke", this.isSmokeDecoy());
    }
}
