package ttv.migami.jeg.vehicle.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.vehicle.data.DefaultVehicleData;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class TestWheelVehicleEntity extends VehicleEntity {
    public TestWheelVehicleEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        this.setVehicleData(DefaultVehicleData.TEST_WHEEL.id());
    }
}
