package ttv.migami.jeg.vehicle.projectile;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.init.ModEntities;

public final class VehicleDecoyEntity extends Entity {
    private static final int LIFE_TICKS = 120;

    public VehicleDecoyEntity(EntityType<? extends VehicleDecoyEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VehicleDecoyEntity(Level level, Vec3 position, Vec3 velocity) {
        this(ModEntities.VEHICLE_DECOY.get(), level);
        this.setPos(position);
        this.setDeltaMovement(velocity);
    }

    public static Optional<VehicleDecoyEntity> findNearest(Level level, Vec3 position, double range) {
        AABB area = new AABB(position, position).inflate(range);
        return level.getEntitiesOfClass(VehicleDecoyEntity.class, area).stream()
                .min(Comparator.comparingDouble(decoy -> decoy.distanceToSqr(position)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFE_TICKS) {
            this.discard();
            return;
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.96D, 0.96D, 0.96D).add(0.0D, -0.006D, 0.0D));
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0.0D, 0.01D, 0.0D);
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
