package ttv.migami.jeg.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.DroneEntity;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModItems;
import ttv.migami.jeg.item.GuidedLauncherItem;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpecialEquipmentGameTests {
    private SpecialEquipmentGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void launcherTargetModes(GameTestHelper helper) {
        ServerPlayer shooter = helper.makeMockServerPlayerInLevel();
        Cow ground = helper.spawn(EntityType.COW, new Vec3(1.5D, 1.0D, 1.5D));
        Phantom air = helper.spawn(EntityType.PHANTOM, new Vec3(1.5D, 5.0D, 1.5D));
        ground.setOnGround(true);

        VehicleMissileProfile javelin = VehicleMissileProfile.get(Reference.id("javelin"));
        VehicleMissileProfile igla = VehicleMissileProfile.get(Reference.id("igla_9k38"));
        helper.assertTrue(javelin.canLock(ground, shooter, null), "Javelin must lock ground living targets");
        helper.assertFalse(javelin.canLock(air, shooter, null), "Javelin must reject airborne targets");
        helper.assertTrue(igla.canLock(air, shooter, null), "Igla must lock airborne targets");
        helper.assertFalse(igla.canLock(ground, shooter, null), "Igla must reject ground targets");

        ItemStack launcher = new ItemStack(ModItems.JAVELIN.get());
        helper.assertValueEqual(GuidedLauncherItem.launcherMode(launcher), 0, "Javelin defaults to direct attack");
        launcher.set(ModDataComponents.LAUNCHER_MODE.get(), 1);
        helper.assertValueEqual(GuidedLauncherItem.launcherMode(launcher), 1, "Javelin stores top-attack mode");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void c4TimerAndOwnership(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        PlacedExplosiveEntity remote = explosive(helper, SpecialExplosiveItem.Kind.C4, first, new Vec3(1.0D, 2.0D, 1.0D));
        remote.setRemote(true);
        PlacedExplosiveEntity timed = explosive(helper, SpecialExplosiveItem.Kind.C4, second, new Vec3(4.0D, 2.0D, 1.0D));
        timed.tickCount = 513;

        helper.assertTrue(remote.isRemoteC4OwnedBy(first.getUUID()), "Owner must match its remote C4");
        helper.assertFalse(remote.isRemoteC4OwnedBy(second.getUUID()), "Other players must not control remote C4");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(timed.isRemoved(), "Timed C4 must detonate at 514 ticks");
            helper.assertFalse(remote.isRemoved(), "Remote C4 must not use the timer");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void claymoreFiltersAndExpires(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        Cow target = helper.spawn(EntityType.COW, new Vec3(1.0D, 2.0D, 3.0D));
        target.setShiftKeyDown(true);
        PlacedExplosiveEntity mine = explosive(helper, SpecialExplosiveItem.Kind.CLAYMORE, owner, new Vec3(1.0D, 2.0D, 1.0D));
        mine.setYRot(0.0F);
        mine.tickCount = 39;
        PlacedExplosiveEntity expired = explosive(helper, SpecialExplosiveItem.Kind.CLAYMORE, owner, new Vec3(4.0D, 2.0D, 1.0D));
        expired.tickCount = 11999;

        helper.runAfterDelay(2, () -> {
            helper.assertFalse(mine.isRemoved(), "Crouching targets must not trigger Claymore");
            helper.assertTrue(expired.isRemoved(), "Claymore must expire at 12000 ticks");
            target.setShiftKeyDown(false);
        });
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(mine.isRemoved(), "Standing target in front must trigger Claymore");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tm62PressureAndTimer(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        PlacedExplosiveEntity pressure = explosive(helper, SpecialExplosiveItem.Kind.TM_62, owner, new Vec3(1.0D, 2.0D, 1.0D));
        pressure.tickCount = 19;
        helper.spawn(EntityType.COW, new Vec3(1.0D, 2.0D, 1.0D));
        PlacedExplosiveEntity timed = explosive(helper, SpecialExplosiveItem.Kind.TM_62, owner, new Vec3(4.0D, 2.0D, 1.0D));
        timed.setTimed(true);
        timed.tickCount = 99;
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(pressure.isRemoved(), "Large entities must trigger armed TM-62");
            helper.assertTrue(timed.isRemoved(), "Timed TM-62 must detonate at 100 ticks");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void droneBindingPayloadAndRange(GameTestHelper helper) {
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ItemStack monitor = new ItemStack(ModItems.MONITOR.get());
        DroneEntity drone = new DroneEntity(helper.getLevel(), owner, helper.absoluteVec(new Vec3(1.0D, 2.0D, 1.0D)));
        helper.getLevel().addFreshEntity(drone);

        owner.setItemInHand(InteractionHand.MAIN_HAND, monitor);
        drone.interact(owner, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(monitor.get(ModDataComponents.DRONE_LINK.get()), drone.getUUID().toString(), "Monitor must store drone UUID");

        ItemStack tm62 = new ItemStack(ModItems.TM_62.get());
        owner.setItemInHand(InteractionHand.MAIN_HAND, tm62);
        drone.interact(owner, InteractionHand.MAIN_HAND);
        helper.assertValueEqual(drone.payloadName(), "TM-62", "Drone must accept one TM-62 payload");

        owner.setItemInHand(InteractionHand.MAIN_HAND, monitor);
        drone.startControl(owner, monitor);
        helper.assertTrue(monitor.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false), "Monitor must enter controlling state");
        drone.processInput(owner, DroneEntity.ACTION_PAYLOAD, 0.0F, 0.0F);
        helper.assertValueEqual(drone.payloadName(), "EMPTY", "Payload action must drop TM-62");
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(PlacedExplosiveEntity.class, drone.getBoundingBox().inflate(2.0D))
                    .stream().anyMatch(entity -> entity.kind() == SpecialExplosiveItem.Kind.TM_62), "Drone must create a TM-62 entity");
            drone.setPos(owner.getX() + 512.0D, owner.getY(), owner.getZ());
            drone.tick();
            helper.assertFalse(monitor.getOrDefault(ModDataComponents.DRONE_CONTROLLING.get(), false), "Out-of-range drone must end control");
            helper.succeed();
        });
    }

    private static PlacedExplosiveEntity explosive(GameTestHelper helper, SpecialExplosiveItem.Kind kind, ServerPlayer owner, Vec3 relativePos) {
        PlacedExplosiveEntity entity = new PlacedExplosiveEntity(helper.getLevel(), kind, owner, helper.absoluteVec(relativePos), 0.0F);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }
}
