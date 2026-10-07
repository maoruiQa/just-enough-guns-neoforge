package ttv.migami.jeg.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;
import ttv.migami.jeg.vehicle.util.VehicleWeaponStats;

@GameTestHolder("jeg_vehicle_damage")
@PrefixGameTestTemplate(false)
public final class LauncherGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rocketBreaksOnlyLavSideComponent(GameTestHelper helper) {
        for (var part : new OBBInfo.Part[]{OBBInfo.Part.WHEEL_LEFT, OBBInfo.Part.WHEEL_RIGHT, OBBInfo.Part.MAIN_ENGINE}) {
            var lav = helper.spawn(ModEntities.LAV150.get(), new Vec3(2, 15, 2));
            lav.setNoGravity(true);
            var center = VehicleGeometry.parts(lav).stream().filter(hit -> hit.part() == part).findFirst().orElseThrow().position();
            var from = center.add(Math.copySign(6, center.x - lav.getX()), 0, 0);
            var shooter = helper.spawn(EntityType.COW, new Vec3(15, 15, 15));
            var rocket = new BulletEntity(helper.getLevel(), shooter, VehicleWeaponStats.get(Reference.id("rocket_launcher")), center.subtract(from));
            rocket.setPos(from);
            helper.getLevel().addFreshEntity(rocket);
            rocket.tick();
            helper.assertTrue(rocket.isRemoved(), "Rocket must collide with LAV " + part);
            helper.assertTrue(lav.partHealthFraction(part) == 0, "One anti-armor rocket must break LAV " + part + ", health fraction=" + lav.partHealthFraction(part));
            helper.assertTrue(part == OBBInfo.Part.MAIN_ENGINE ? lav.isEngineDamaged() : part == OBBInfo.Part.WHEEL_LEFT ? lav.isLeftWheelDamaged() : lav.isRightWheelDamaged(), "The damaged component must publish its HUD fault flag");
            for (var other : new OBBInfo.Part[]{OBBInfo.Part.WHEEL_LEFT, OBBInfo.Part.WHEEL_RIGHT, OBBInfo.Part.MAIN_ENGINE, OBBInfo.Part.TURRET}) {
                if (other != part) helper.assertValueEqual(lav.partHealthFraction(other), 1.0F, "Unhit component " + other);
            }
            System.out.println("LAUNCHER_CHECK LAV side=" + part + " hull=" + lav.vehicleHealth());
            lav.discard();
            shooter.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rocketVehicleBalance(GameTestHelper helper) {
        var types = List.of(ModEntities.TRUCK.get(), ModEntities.LAV150.get(), ModEntities.BMP2.get(), ModEntities.SPEEDBOAT.get(), ModEntities.AH6.get(), ModEntities.MI28.get(), ModEntities.A10.get(), ModEntities.TOM6.get(), ModEntities.LASER_TOWER.get(), ModEntities.HPJ11.get(), ModEntities.WAVEFORCE_TOWER.get());
        for (var type : types) {
            var vehicle = helper.spawn(type, new Vec3(2, 15, 2));
            vehicle.setNoGravity(true);
            var shooter = helper.spawn(EntityType.COW, new Vec3(15, 15, 15));
            int shots = 0;
            while (!vehicle.isRemoved() && shots < 12) {
                var center = VehicleGeometry.parts(vehicle).stream().filter(hit -> hit.part() == OBBInfo.Part.BODY).findFirst().orElseThrow().position();
                var from = center.add(12, 0, 0);
                var rocket = new BulletEntity(helper.getLevel(), shooter, VehicleWeaponStats.get(Reference.id("rocket_launcher")), center.subtract(from));
                rocket.setPos(from);
                helper.getLevel().addFreshEntity(rocket);
                rocket.tick();
                helper.assertTrue(rocket.isRemoved(), "Rocket must hit " + vehicle.vehicleDataId());
                shots++;
            }
            System.out.println("LAUNCHER_BALANCE " + vehicle.vehicleDataId() + " shots=" + shots + " remaining=" + vehicle.vehicleHealth());
            helper.assertTrue(vehicle.isRemoved() && shots <= 5, "Rocket must destroy " + vehicle.vehicleDataId() + " within 5 side hits; shots=" + shots);
            vehicle.discard();
            shooter.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void javelinGuidedDirectImpact(GameTestHelper helper) { flyingMissile(helper, "javelin", false, true); }
    @GameTest(template = "empty", timeoutTicks = 140)
    public static void javelinTopAttackImpact(GameTestHelper helper) { flyingMissile(helper, "javelin", true, true); }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void javelinUnguidedImpact(GameTestHelper helper) { flyingMissile(helper, "javelin", false, false); }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void iglaAirImpact(GameTestHelper helper) { flyingMissile(helper, "igla_9k38", false, true); }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void missileExcludesOnlyOwnVehicleAndCrew(GameTestHelper helper) {
        var own = helper.spawn(ModEntities.LAV150.get(), new Vec3(2, 15, 2));
        var other = helper.spawn(ModEntities.LAV150.get(), new Vec3(12, 15, 2));
        var shooter = helper.spawn(EntityType.COW, new Vec3(2, 15, 2));
        shooter.startRiding(own, true);
        var from = own.position().add(0, 1, 0);
        var missile = new VehicleMissileEntity(helper.getLevel(), shooter, null, null, from, new Vec3(12, 0, 0), Reference.id("javelin"), false);
        helper.getLevel().addFreshEntity(missile);
        missile.tick();
        helper.assertTrue(missile.isRemoved() && other.vehicleHealth() < 270, "Seated launcher must still hit another vehicle");
        helper.assertValueEqual(own.vehicleHealth(), 270.0F, "Own vehicle must remain excluded");
        helper.assertTrue(shooter.isAlive(), "Own crew must remain excluded");
        own.discard(); other.discard(); shooter.discard(); helper.succeed();
    }

    private static void flyingMissile(GameTestHelper helper, String weapon, boolean top, boolean guided) {
        var vehicle = helper.spawn(weapon.equals("igla_9k38") ? ModEntities.AH6.get() : ModEntities.LAV150.get(), new Vec3(2, 25, 2));
        vehicle.setNoGravity(true);
        Vec3 anchor = vehicle.position();
        var shooter = helper.spawn(EntityType.COW, new Vec3(2, 25, -43));
        shooter.setNoAi(true);
        shooter.setNoGravity(true);
        var aim = anchor.add(0, vehicle.getBbHeight() * .5, 0);
        // Unguided flight drops 1.8 blocks over this 45-block shot; aim above the target.
        var from = aim.add(0, guided ? 0 : 1.8, -45);
        var direction = new Vec3(0, 0, 1);
        var velocity = guided ? direction.add(0, .3, 0).normalize().scale(3) : direction.scale(5);
        var missile = new VehicleMissileEntity(helper.getLevel(), shooter, guided ? vehicle : null, guided ? aim : null, from, velocity, Reference.id(weapon), top);
        helper.getLevel().addFreshEntity(missile);
        float health = vehicle.vehicleHealth();
        // Step the production tick; the empty GameTest structure does not tick distant launch chunks.
        int limit = top ? 130 : guided ? 90 : 35;
        for (int tick = 0; tick < limit && !missile.isRemoved(); tick++) {
            missile.tickCount++;
            missile.tick();
        }
        helper.assertTrue(missile.isRemoved(), weapon + " must collide before lifetime expiry: pos=" + missile.position());
        helper.assertTrue(vehicle.vehicleHealth() < health, weapon + " must detonate on the vehicle before the flight timeout");
        System.out.println("LAUNCHER_FLIGHT " + weapon + " top=" + top + " guided=" + guided + " ticks=" + missile.tickCount + " damage=" + (health - vehicle.vehicleHealth()));
        vehicle.discard(); shooter.discard(); helper.succeed();
    }
}
