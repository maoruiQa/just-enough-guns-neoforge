package ttv.migami.jeg.vehicle.data.subdata;

import net.minecraft.resources.ResourceLocation;

public record VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided, int seatIndex, double muzzleX, double muzzleY, double muzzleZ) {
    public VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided) {
        this(weaponId, ammoId, energyCost, guided, -1);
    }

    public VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided, int seatIndex) {
        this(weaponId, ammoId, energyCost, guided, seatIndex, Double.NaN, Double.NaN, Double.NaN);
    }

    public boolean usableBySeat(int seatIndex) {
        return this.seatIndex < 0 || this.seatIndex == seatIndex;
    }

    public boolean hasMuzzle() {
        return !Double.isNaN(this.muzzleX) && !Double.isNaN(this.muzzleY) && !Double.isNaN(this.muzzleZ);
    }
}
