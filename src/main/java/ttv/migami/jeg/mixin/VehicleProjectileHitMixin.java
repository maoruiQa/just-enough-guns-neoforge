package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.util.VehiclePartHit;

@Mixin(Projectile.class)
public abstract class VehicleProjectileHitMixin implements VehiclePartHit {
    @Unique private int jeg$vehicleId = -1;
    @Unique private OBBInfo.Part jeg$part;

    public void jeg$setVehiclePartHit(int entityId, OBBInfo.Part part) {
        this.jeg$vehicleId = entityId;
        this.jeg$part = part;
    }

    public OBBInfo.Part jeg$getVehiclePartHit(int entityId) {
        return entityId == this.jeg$vehicleId ? this.jeg$part : null;
    }
}
