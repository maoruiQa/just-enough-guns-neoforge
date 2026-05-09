package ttv.migami.jeg.vehicle.data.subdata;

import net.minecraft.resources.ResourceLocation;

public record VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided) {
}
