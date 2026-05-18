package ttv.migami.jeg.vehicle.entity;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class ConfiguredVehicleEntity extends VehicleEntity {
    public ConfiguredVehicleEntity(EntityType<? extends VehicleEntity> type, Level level, Identifier vehicleDataId) {
        super(type, level);
        this.setVehicleData(vehicleDataId);
    }
}
