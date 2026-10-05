package ttv.migami.jeg.gametest;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MoverType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;
import ttv.migami.jeg.vehicle.util.VehicleGeometry;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VehicleCollisionGameTests {
    private VehicleCollisionGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void impactFeedbackSurvivesDamageCooldown(GameTestHelper helper) throws ReflectiveOperationException {
        ConfiguredVehicleEntity bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(1.5D, 1.0D, 1.5D));
        helper.setBlock(new BlockPos(4, 1, 1), Blocks.STONE);
        float health = bmp.vehicleHealth();
        setField(bmp, "ramDamageCooldown", 10);
        setField(bmp, "enginePower", 1.0D);
        bmp.setDeltaMovement(0.6D, 0.0D, 0.0D);
        double startX = bmp.getX();

        bmp.move(MoverType.SELF, new Vec3(0.6D, 0.0D, 0.0D));

        helper.assertTrue(bmp.getX() < startX + 0.6D - 1.0E-4D, "The block must physically stop the vehicle");
        helper.assertValueEqual(bmp.vehicleHealth(), health, "Damage cooldown must still protect hull health");
        helper.assertTrue((double) getField(bmp, "enginePower") < 1.0D, "Collision must damp engine power despite damage cooldown");
        helper.assertValueEqual(getField(bmp, "lastVehicleStrikeSoundTick"), bmp.tickCount,
                "Collision must emit the strike sound despite damage cooldown");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swSizedBaseAndObbNarrowPhase(GameTestHelper helper) {
        ConfiguredVehicleEntity bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(2.0D, 1.0D, 2.0D));
        bmp.setYRot(45.0F);
        bmp.refreshDimensions();
        helper.assertTrue(Math.abs(bmp.getBoundingBox().getXsize() - 4.0D) < 1.0E-4D,
                "BMP2 block collision width must match SW's four blocks");

        AABB broad = VehicleGeometry.bounds(bmp);
        boolean foundBroadOnly = false;
        double y = bmp.getY() + 1.0D;
        for (double x = broad.minX; x < broad.maxX && !foundBroadOnly; x += 0.2D) {
            for (double z = broad.minZ; z < broad.maxZ; z += 0.2D) {
                AABB probe = new AABB(x, y, z, x + 0.08D, y + 0.08D, z + 0.08D);
                if (broad.intersects(probe) && !VehicleGeometry.intersects(bmp, probe)) {
                    foundBroadOnly = true;
                    break;
                }
            }
        }
        helper.assertTrue(foundBroadOnly, "Dynamic OBB narrow phase must reject broad-phase corner contacts");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void drivingProgressRequiresPlayerInput(GameTestHelper helper) throws ReflectiveOperationException {
        ServerPlayer driver = helper.makeMockServerPlayerInLevel();
        LivingEntity[] controller = {null};
        VehicleEntity bmp = new VehicleEntity(ModEntities.BMP2.get(), helper.getLevel()) {
            @Override
            public LivingEntity getControllingPassenger() {
                return controller[0];
            }
        };
        helper.assertFalse(bmp.hasDrivingInput(), "A vehicle pushed without a player driver must not count as driving");
        controller[0] = driver;
        helper.assertFalse(bmp.hasDrivingInput(), "A player without throttle must not count as driving");
        setField(bmp, "input", new VehicleInput(true, false, false, false, false, false, false,
                false, false, false, false, false, -1, false, false, 0.0F, 0.0F));
        helper.assertTrue(bmp.hasDrivingInput(), "Player control with real input must count as driving");
        controller[0] = null;
        helper.assertFalse(bmp.hasDrivingInput(), "Losing the driver must stop driving progress");
        helper.succeed();
    }

    private static void setField(VehicleEntity vehicle, String name, Object value) throws ReflectiveOperationException {
        Field field = VehicleEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(vehicle, value);
    }

    private static Object getField(VehicleEntity vehicle, String name) throws ReflectiveOperationException {
        Field field = VehicleEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(vehicle);
    }
}
