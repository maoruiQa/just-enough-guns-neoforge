package ttv.migami.jeg.gametest;

import java.lang.reflect.Method;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@GameTestHolder("jeg_vehicle_damage")
@PrefixGameTestTemplate(false)
public final class VehicleMuzzleGameTests {
    private VehicleMuzzleGameTests() {}

    /**
     * With the turret centred, every SW barrel/turret muzzle must sit at the barrel tip:
     * the vehicle-space point (turret origin + barrel offset + SW ShootPos). Storing that
     * pre-summed point in the JSON put the muzzle flash ~2.7 blocks above the barrel.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swMuzzlesSitOnBarrelTips(GameTestHelper helper) throws ReflectiveOperationException {
        check(helper, ModEntities.LAV150.get(), 0, new Vec3(0.0434375, 2.78995, 4.025));
        check(helper, ModEntities.LAV150.get(), 1, new Vec3(0.3234375, 2.78995, 1.325));
        check(helper, ModEntities.BMP2.get(), 0, new Vec3(-0.0875, 2.943125, 3.677825));
        check(helper, ModEntities.BMP2.get(), 1, new Vec3(0.1625, 2.843125, 0.977825));
        check(helper, ModEntities.BMP2.get(), 2, new Vec3(0.0, 2.25 + 1.24375, -0.703125 - 0.3234));
        check(helper, ModEntities.SPEEDBOAT.get(), 0, new Vec3(0.0, 3.2705, 0.4761));
        helper.succeed();
    }

    private static void check(GameTestHelper helper, EntityType<? extends VehicleEntity> type, int slot, Vec3 expectedLocal)
            throws ReflectiveOperationException {
        VehicleEntity vehicle = helper.spawn(type, new Vec3(2, 15, 2));
        vehicle.setNoGravity(true);
        VehicleWeaponInfo weapon = vehicle.vehicleData().defaults().weapons().get(slot);
        Method method = VehicleEntity.class.getDeclaredMethod("weaponMuzzlePosition", VehicleWeaponInfo.class, Vec3.class, double.class, double.class);
        method.setAccessible(true);
        Vec3 muzzle = ((Vec3) method.invoke(vehicle, weapon, vehicle.getViewVector(1.0F), 1.25D, 0.95D)).subtract(vehicle.position());
        double horizontal = Math.sqrt(muzzle.x * muzzle.x + muzzle.z * muzzle.z);
        double expectedHorizontal = Math.sqrt(expectedLocal.x * expectedLocal.x + expectedLocal.z * expectedLocal.z);
        String label = vehicle.vehicleDataId() + " slot " + slot + " " + weapon.weaponId() + " muzzle=" + muzzle;
        System.out.println("MUZZLE_CHECK " + label + " expected=" + expectedLocal);
        helper.assertTrue(Math.abs(muzzle.y - expectedLocal.y) < 0.02D, "Muzzle height must match the barrel tip: " + label);
        helper.assertTrue(Math.abs(horizontal - expectedHorizontal) < 0.02D, "Muzzle reach must match the barrel tip: " + label);
        vehicle.discard();
    }
}
