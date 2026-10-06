package ttv.migami.jeg.vehicle.data.subdata;

import net.minecraft.world.phys.Vec3;

import net.minecraft.resources.ResourceLocation;

public record VehicleWeaponInfo(
        ResourceLocation weaponId,
        ResourceLocation ammoId,
        int energyCost,
        boolean guided,
        int seatIndex,
        double muzzleX,
        double muzzleY,
        double muzzleZ,
        float defaultZoom,
        String crosshair,
        String crosshairZooming,
        int crosshairColor,
        ResourceLocation icon,
        String hudTransform,
        Vec3 hudPosition,
        String hudDirection,
        Vec3 hudDirectionVector,
        String viewDirection,
        Vec3 viewPosition
) {
    public VehicleWeaponInfo(ResourceLocation weaponId, ResourceLocation ammoId, int energyCost, boolean guided, int seatIndex, double muzzleX, double muzzleY, double muzzleZ) {
        this(weaponId, ammoId, energyCost, guided, seatIndex, muzzleX, muzzleY, muzzleZ, 3.0F, "", "", 0xFFFFFF, null, "vehicle", null, "default", null, "default", null);
    }

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
