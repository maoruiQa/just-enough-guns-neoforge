package ttv.migami.jeg.vehicle.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleEnergyStorage implements IEnergyStorage {
    private final VehicleEntity vehicle;

    public VehicleEnergyStorage(VehicleEntity vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = Math.min(maxReceive, this.getMaxEnergyStored() - this.getEnergyStored());
        if (received <= 0) {
            return 0;
        }
        if (!simulate) {
            this.vehicle.addEnergy(received);
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.vehicle.vehicleEnergy();
    }

    @Override
    public int getMaxEnergyStored() {
        return this.vehicle.maxVehicleEnergy();
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return this.getMaxEnergyStored() > 0;
    }
}
