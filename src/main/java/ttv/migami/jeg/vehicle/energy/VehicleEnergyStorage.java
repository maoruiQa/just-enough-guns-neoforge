package ttv.migami.jeg.vehicle.energy;

import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleEnergyStorage {
    private final VehicleEntity vehicle;

    public VehicleEnergyStorage(VehicleEntity vehicle) {
        this.vehicle = vehicle;
    }

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

    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    public int getEnergyStored() {
        return this.vehicle.vehicleEnergy();
    }

    public int getMaxEnergyStored() {
        return this.vehicle.maxVehicleEnergy();
    }

    public boolean canExtract() {
        return false;
    }

    public boolean canReceive() {
        return this.getMaxEnergyStored() > 0;
    }

    public long getAmountAsLong() {
        return this.getEnergyStored();
    }

    public long getCapacityAsLong() {
        return this.getMaxEnergyStored();
    }
}
