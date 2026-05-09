package ttv.migami.jeg.vehicle.data;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.data.subdata.CollisionLevel;
import ttv.migami.jeg.vehicle.data.subdata.DamageModifierInfo;
import ttv.migami.jeg.vehicle.data.subdata.DestroyInfo;
import ttv.migami.jeg.vehicle.data.subdata.DismountInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.data.subdata.EngineType;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeatInfo;
import ttv.migami.jeg.vehicle.data.subdata.SeekInfo;
import ttv.migami.jeg.vehicle.data.subdata.VehicleContainerType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;

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
        DismountInfo dismount,
        CollisionLevel collisionLevel,
        OBBInfo obb,
        VehicleArmorProfile armor,
        DamageModifierInfo damageModifier,
        List<VehicleWeaponInfo> weapons,
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
            DismountInfo.DEFAULT,
            CollisionLevel.MEDIUM,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("assault_rifle"), Reference.id("rifle_ammo"), 1, false),
                    new VehicleWeaponInfo(Reference.id("combat_pistol"), Reference.id("pistol_ammo"), 0, false)
            ),
            SeekInfo.NONE,
            DestroyInfo.NONE
    );

    public static final DefaultVehicleData LIGHT_COMBAT = new DefaultVehicleData(
            Reference.id("light_combat_vehicle"),
            "jeg:light_combat_vehicle",
            VehicleType.LAND,
            80.0F,
            0.015F,
            140,
            160,
            new EngineInfo(EngineInfo.WHEEL_TEST.type(), 0.038D, 0.34D, 0.14D, 0.80D, 0.0D),
            List.of(new SeatInfo(0, 0.0D, 0.75D, 0.0D, true, true)),
            true,
            VehicleContainerType.NONE,
            new CameraPos(0.0D, 2.8D, -6.0D),
            DismountInfo.DEFAULT,
            CollisionLevel.HEAVY,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("assault_rifle"), Reference.id("rifle_ammo"), 1, false),
                    new VehicleWeaponInfo(Reference.id("combat_pistol"), Reference.id("pistol_ammo"), 0, false),
                    new VehicleWeaponInfo(Reference.id("rocket_launcher"), Reference.id("rocket"), 2, true)
            ),
            SeekInfo.NONE,
            DestroyInfo.NONE
    );

    public static final DefaultVehicleData TEST_HELICOPTER = new DefaultVehicleData(
            Reference.id("test_helicopter"),
            "jeg:test_helicopter",
            VehicleType.HELICOPTER,
            60.0F,
            0.012F,
            120,
            140,
            new EngineInfo(EngineType.ROTOR, 0.035D, 0.48D, 0.18D, 0.92D, 0.0D),
            List.of(new SeatInfo(0, 0.0D, 0.85D, 0.0D, true, false)),
            true,
            VehicleContainerType.NONE,
            new CameraPos(0.0D, 3.2D, -7.0D),
            DismountInfo.DEFAULT,
            CollisionLevel.MEDIUM,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("combat_pistol"), Reference.id("pistol_ammo"), 0, false),
                    new VehicleWeaponInfo(Reference.id("rocket_launcher"), Reference.id("rocket"), 2, true)
            ),
            SeekInfo.NONE,
            new DestroyInfo(true, 2.0F)
    );

    public static final DefaultVehicleData TEST_BOAT = new DefaultVehicleData(
            Reference.id("test_boat"),
            "jeg:test_boat",
            VehicleType.BOAT,
            55.0F,
            0.012F,
            120,
            100,
            new EngineInfo(EngineType.BOAT, 0.035D, 0.36D, 0.14D, 0.86D, 0.0D),
            List.of(new SeatInfo(0, 0.0D, 0.72D, 0.0D, true, false)),
            true,
            VehicleContainerType.NONE,
            new CameraPos(0.0D, 2.8D, -6.0D),
            DismountInfo.DEFAULT,
            CollisionLevel.LIGHT,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("combat_pistol"), Reference.id("pistol_ammo"), 0, false)
            ),
            SeekInfo.NONE,
            new DestroyInfo(false, 0.0F)
    );

    public static final DefaultVehicleData TEST_ARTILLERY = new DefaultVehicleData(
            Reference.id("test_artillery"),
            "jeg:test_artillery",
            VehicleType.ARTILLERY,
            70.0F,
            0.01F,
            140,
            120,
            new EngineInfo(EngineType.STATIC, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D),
            List.of(new SeatInfo(0, 0.0D, 0.9D, 0.0D, true, true)),
            true,
            VehicleContainerType.NONE,
            new CameraPos(0.0D, 2.6D, -5.0D),
            DismountInfo.DEFAULT,
            CollisionLevel.HEAVY,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("light_machine_gun"), Reference.id("rifle_ammo"), 1, false),
                    new VehicleWeaponInfo(Reference.id("rocket_launcher"), Reference.id("rocket"), 2, true)
            ),
            SeekInfo.NONE,
            new DestroyInfo(true, 2.2F)
    );

    public static final DefaultVehicleData TEST_AIRCRAFT = new DefaultVehicleData(
            Reference.id("test_aircraft"),
            "jeg:test_aircraft",
            VehicleType.AIRCRAFT,
            65.0F,
            0.01F,
            120,
            150,
            new EngineInfo(EngineType.JET, 0.03D, 0.65D, 0.12D, 0.94D, 0.0D),
            List.of(new SeatInfo(0, 0.0D, 0.8D, 0.0D, true, false)),
            true,
            VehicleContainerType.NONE,
            new CameraPos(0.0D, 3.0D, -8.0D),
            DismountInfo.DEFAULT,
            CollisionLevel.MEDIUM,
            OBBInfo.DEFAULT,
            VehicleArmorProfile.LIGHT,
            DamageModifierInfo.DEFAULT,
            List.of(
                    new VehicleWeaponInfo(Reference.id("light_machine_gun"), Reference.id("rifle_ammo"), 1, false),
                    new VehicleWeaponInfo(Reference.id("rocket_launcher"), Reference.id("rocket"), 2, true)
            ),
            SeekInfo.NONE,
            new DestroyInfo(true, 2.4F)
    );
}
