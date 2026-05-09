package ttv.migami.jeg.vehicle.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ttv.migami.jeg.vehicle.data.DefaultVehicleData;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class TestArtilleryEntity extends VehicleEntity {
    public TestArtilleryEntity(EntityType<? extends VehicleEntity> type, Level level) {
        super(type, level);
        this.setVehicleData(DefaultVehicleData.TEST_ARTILLERY.id());
    }
}
