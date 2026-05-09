package ttv.migami.jeg.vehicle.data.subdata;

import net.minecraft.resources.ResourceLocation;

public record VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided, int seatIndex) {
    public VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided) {
        this(weaponId, ammoId, energyCost, guided, -1);
    }

    public boolean usableBySeat(int seatIndex) {
        return this.seatIndex < 0 || this.seatIndex == seatIndex;
    }
}
