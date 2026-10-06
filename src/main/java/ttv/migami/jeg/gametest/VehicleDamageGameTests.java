package ttv.migami.jeg.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.BulletEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.util.VehicleWeaponStats;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;
import ttv.migami.jeg.vehicle.util.VehiclePartHit;

@GameTestHolder("jeg_vehicle_damage")
@PrefixGameTestTemplate(false)
public final class VehicleDamageGameTests {
    private VehicleDamageGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void engineRecoveryAndSaveMigration(GameTestHelper helper) throws ReflectiveOperationException {
        var bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(2, 1, 2));
        var source = helper.getLevel().damageSources().generic();
        helper.assertTrue(bmp.hurtAtPart(source, 400, OBBInfo.Part.MAIN_ENGINE), "The shared part damage path must accept the hit");
        helper.assertTrue(bmp.isEngineDamaged(), "A hit exceeding 50 component health must break the engine");
        helper.assertTrue(Math.abs(bmp.vehicleHealth() - 222.6F) < .01F, "SW reduction must precede multiplication");
        bmp.repairWithTool(0, 30);
        helper.assertTrue(bmp.isEngineDamaged(), "Repairing above zero must not clear the SW fault");
        CompoundTag saved = bmp.saveVehicleContainerState();
        var restored = helper.spawn(ModEntities.BMP2.get(), new Vec3(2, 1, 2));
        restored.loadVehicleContainerState(saved);
        helper.assertTrue(restored.isEngineDamaged(), "A save round trip must preserve recovery hysteresis");
        restored.repairWithTool(0, 45);
        helper.assertFalse(restored.isEngineDamaged(), "Recovering above 95 percent must clear the fault");
        saved.remove("PartDamageVersion");
        saved.remove("PartDamageFlags");
        saved.putFloat("EngineHealth", 5);
        restored.loadVehicleContainerState(saved);
        helper.assertTrue(Math.abs(partHealth(restored, "engineHealth") - 25) < .01F, "Old 10-point saves must migrate proportionally");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void vanillaProjectileCarriesA10EngineHit(GameTestHelper helper) throws ReflectiveOperationException {
        var aircraft = helper.spawn(ModEntities.A10.get(), new Vec3(2, 1, 2));
        helper.assertValueEqual(aircraft.vehicleData().defaults().obb().boxes().size(), 11, "A10 must contain every SW OBB");
        Vec3 from = aircraft.position().add(-6, 3.375, -3.5);
        Vec3 to = aircraft.position().add(-1.625, 3.375, -3.5);
        var hit = VehicleGeometry.clip(aircraft, from, to);
        helper.assertTrue(hit != null && hit.part() == OBBInfo.Part.MAIN_ENGINE, "A10 nacelle must resolve to its own engine");
        var arrow = helper.spawn(EntityType.ARROW, new Vec3(2, 1, 2));
        arrow.setPos(from);
        var result = ProjectileUtil.getEntityHitResult(helper.getLevel(), arrow, from, to, new AABB(from, to).inflate(1), entity -> entity == aircraft, 0.0F);
        helper.assertTrue(result != null && result.getEntity() == aircraft, "Vanilla projectile picking must reach SW geometry");
        helper.assertTrue(((VehiclePartHit) arrow).jeg$getVehiclePartHit(aircraft.getId()) == OBBInfo.Part.MAIN_ENGINE,
                "Projectile mixin must retain the selected part until damage processing");
        aircraft.hurt(helper.getLevel().damageSources().arrow(arrow, null), 40);
        helper.assertTrue(partHealth(aircraft, "engineHealth") < 50, "Vanilla arrow damage must reach the selected part");
        helper.assertValueEqual(partHealth(aircraft, "subEngineHealth"), 50.0F, "The other engine must remain intact");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void independentSubEngineRepairAndHullDecay(GameTestHelper helper) throws ReflectiveOperationException {
        var helicopter = helper.spawn(ModEntities.AH6.get(), new Vec3(2, 1, 2));
        helicopter.hurtAtPart(helper.getLevel().damageSources().generic(), 110, OBBInfo.Part.SUB_ENGINE);
        float damagedHealth = partHealth(helicopter, "subEngineHealth");
        helper.assertTrue(helicopter.isSubEngineDamaged(), "Tail rotor must have independent damage state");
        float hull = helicopter.vehicleHealth();
        Method tickRepair = VehicleEntity.class.getDeclaredMethod("tickAutoRepair");
        tickRepair.setAccessible(true);
        tickRepair.invoke(helicopter);
        helper.assertTrue(Math.abs(partHealth(helicopter, "subEngineHealth") - damagedHealth - .125F) < .001F,
                "Tail rotor must regenerate during hull repair cooldown");
        helper.assertValueEqual(helicopter.vehicleHealth(), hull, "Hull repair cooldown must still apply");
        CompoundTag saved = helicopter.saveVehicleContainerState();
        saved.putFloat("Health", 25);
        helicopter.loadVehicleContainerState(saved);
        tickRepair.invoke(helicopter);
        helper.assertTrue(Math.abs(helicopter.vehicleHealth() - 24.9F) < .001F, "Hull at ten percent must lose 0.1 health every tick");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void immunityAndPassengerDestruction(GameTestHelper helper) {
        var tower = helper.spawn(ModEntities.LASER_TOWER.get(), new Vec3(2, 1, 2));
        float health = tower.vehicleHealth();
        helper.assertFalse(tower.hurt(helper.getLevel().damageSources().cactus(), 100), "SW immune damage tag must apply");
        helper.assertFalse(tower.hurt(helper.getLevel().damageSources().drown(), 100), "Default SW damage immunity must apply");
        helper.assertValueEqual(tower.vehicleHealth(), health, "Immune damage must leave hull unchanged");
        var passenger = helper.spawn(EntityType.COW, new Vec3(2, 1, 2));
        passenger.startRiding(tower, true);
        helper.assertTrue(passenger.getVehicle() == tower, "Passenger must occupy the destroyed vehicle");
        tower.hurt(helper.getLevel().damageSources().genericKill(), 10000);
        helper.assertTrue(tower.isRemoved(), "Hull destruction must discard the vehicle once");
        helper.assertFalse(passenger.isAlive(), "SW passenger destruction must run before the blast");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void projectileFamiliesAndCannonPartDamage(GameTestHelper helper) throws ReflectiveOperationException {
        var bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(2, 1, 2));
        var modifier = bmp.vehicleData().defaults().damageModifier();
        var missile = new VehicleMissileEntity(ModEntities.VEHICLE_MISSILE.get(), helper.getLevel());
        helper.assertTrue(Math.abs(modifier.apply(helper.getLevel().damageSources().explosion(missile, null), 100) - 34.8F) < .001F,
                "A missile blast must match projectile_explosion alone, never stack vanilla explosion x6");
        helper.assertTrue(Math.abs(modifier.apply(helper.getLevel().damageSources().explosion(null, null), 100) - 104.4F) < .001F,
                "An ordinary explosion must retain the vanilla SW multiplier");
        var shooter = helper.spawn(EntityType.COW, new Vec3(10, 1, 10));
        var shell = new BulletEntity(helper.getLevel(), shooter, VehicleWeaponStats.get(Reference.id("vehicle_20mm_cannon")), Vec3.ZERO);
        helper.assertTrue(Math.abs(modifier.apply(helper.getLevel().damageSources().thrown(shell, shooter), 100) - 23.49F) < .001F,
                "Cannon direct damage must match projectile_hit without the normal bullet tag");
        helper.assertTrue(Math.abs(modifier.apply(helper.getLevel().damageSources().explosion(shell, shooter), 100) - 34.8F) < .001F,
                "Cannon blast must match custom_explosion alone");
        helper.assertTrue(Math.abs(modifier.apply(ModDamageTypes.causeVehicleBlastDamage(helper.getLevel().registryAccess(), bmp, null), 100) - 34.8F) < .001F,
                "Vehicle destruction blast must use the custom explosion family");
        var engine = VehicleGeometry.parts(bmp).stream().filter(part -> part.part() == OBBInfo.Part.MAIN_ENGINE).findFirst().orElseThrow();
        Field hit = BulletEntity.class.getDeclaredField("vehicleHit");
        hit.setAccessible(true);
        hit.set(shell, engine);
        shell.setPos(engine.position());
        Method impact = BulletEntity.class.getDeclaredMethod("handleSpecialImpact", HitResult.class);
        impact.setAccessible(true);
        impact.invoke(shell, new EntityHitResult(bmp, engine.position()));
        helper.assertTrue(partHealth(bmp, "engineHealth") < 50,
                "The actual cannon impact path must apply direct engine damage despite its separate blast");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void truckFallsDamageChassisWithCreativeDriver(GameTestHelper helper) throws ReflectiveOperationException {
        java.util.List<VehicleEntity> trucks = new java.util.ArrayList<>();
        int[] heights = {2, 5, 20};
        float[] expectedHealth = {250, 250 - 250 * 2.0F / 17, 0};
        boolean[] landed = new boolean[heights.length];
        for (int i = 0; i < heights.length; i++) {
            var floor = helper.absolutePos(new net.minecraft.core.BlockPos(24 + i * 48, 1, 24));
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                helper.getLevel().setChunkForced((floor.getX() + x) >> 4, (floor.getZ() + z) >> 4, true);
                helper.getLevel().setBlockAndUpdate(floor.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
            var truck = ModEntities.TRUCK.get().create(helper.getLevel());
            if (heights[i] == 20) {
                Field cooldown = VehicleEntity.class.getDeclaredField("ramDamageCooldown");
                cooldown.setAccessible(true);
                cooldown.setInt(truck, 1000);
            }
            truck.setPos(floor.getX() + .5, floor.getY() + 1 + heights[i], floor.getZ() + .5);
            helper.getLevel().addFreshEntity(truck);
            var driver = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
            net.minecraft.world.level.GameType.CREATIVE.updatePlayerAbilities(driver.getAbilities());
            driver.setPos(truck.position());
            helper.assertTrue(driver.startRiding(truck, true), "Creative driver must occupy the truck");
            trucks.add(truck);
        }
        helper.onEachTick(() -> {
            boolean allLanded = true;
            for (int i = 0; i < trucks.size(); i++) {
                VehicleEntity truck = trucks.get(i);
                if (!landed[i] && (truck.isRemoved() || truck.onGround())) {
                    helper.assertTrue(Math.abs(truck.vehicleHealth() - expectedHealth[i]) < .01F,
                            "Truck health after " + heights[i] + " block drop: " + truck.vehicleHealth());
                    helper.assertTrue(truck.isRemoved() == (heights[i] == 20), "Only the severe fall must destroy the truck");
                    if (heights[i] == 2) {
                        Vec3 supportedMotion = new Vec3(.1, -.06, 0);
                        truck.setDeltaMovement(supportedMotion);
                        truck.move(net.minecraft.world.entity.MoverType.SELF, supportedMotion);
                        helper.assertValueEqual(truck.vehicleHealth(), 250.0F, "Driving on supported ground must not damage the chassis");
                    }
                    landed[i] = true;
                    truck.ejectPassengers();
                    truck.discard();
                }
                allLanded &= landed[i];
            }
            if (allLanded) helper.succeed();
        });
    }

    private static float partHealth(VehicleEntity vehicle, String name) throws ReflectiveOperationException {
        Field field = VehicleEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(vehicle);
    }
}
