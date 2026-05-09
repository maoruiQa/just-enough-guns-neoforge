package ttv.migami.jeg.vehicle.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.data.subdata.DestroyInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeekInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleContainerType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;

public record DefaultVehicleData(
        ResourceLocation id,
        String entityType,
        VehicleType vehicleType,
        float maxHealth,
        float autoRepairPerTick,
        int autoRepairCooldownTicks,
        int maxEnergy,
        EngineInfo engine,
        List<SeatInfo> seats,
        boolean allowFreeCam,
        VehicleContainerType containerType,
        CameraPos thirdPersonCamera,
        OBBInfo obb,
        VehicleArmorProfile armor,
        SeekInfo seek,
        DestroyInfo destroy
) {
    public static final DefaultVehicleData TEST_WHEEL = new DefaultVehicleData(
            Reference.id("test_wheel_vehicle"),
            "jeg:test_wheel_vehicle",
            VehicleType.LAND,
            40.0F,
            0.02F,
            100,
            100,
            EngineInfo.WHEEL_TEST,
            List.of(SeatInfo.DRIVER),
            true,
            VehicleContainerType.NONE,
            CameraPos.DEFAULT,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            SeekInfo.NONE,
            DestroyInfo.NONE
    );
}
