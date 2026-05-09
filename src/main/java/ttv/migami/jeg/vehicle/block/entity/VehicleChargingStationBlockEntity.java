package ttv.migami.jeg.vehicle.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import ttv.migami.jeg.init.ModBlockEntities;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleChargingStationBlockEntity extends BlockEntity {
    private static final int CHARGE_INTERVAL = 20;
    private static final int CHARGE_PER_INTERVAL = 8;
    private int chargeTick;

    public VehicleChargingStationBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.VEHICLE_CHARGING_STATION.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VehicleChargingStationBlockEntity station) {
        station.chargeTick++;
        if (station.chargeTick < CHARGE_INTERVAL) {
            return;
        }
        station.chargeTick = 0;

        AABB range = new AABB(pos).inflate(2.0D, 1.0D, 2.0D).expandTowards(0.0D, 2.0D, 0.0D);
        for (VehicleEntity vehicle : level.getEntitiesOfClass(VehicleEntity.class, range)) {
            var energy = vehicle.getCapability(Capabilities.EnergyStorage.ENTITY, null);
            if (energy != null && energy.receiveEnergy(CHARGE_PER_INTERVAL, false) > 0) {
                station.setChanged();
                return;
            }
        }
    }
}
