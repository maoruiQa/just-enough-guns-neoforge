package ttv.migami.jeg.gametest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.data.subdata.EngineInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;

@GameTestHolder("jeg_vehicle_controls")
@PrefixGameTestTemplate(false)
public final class VehicleControlGameTests {
    private VehicleControlGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aiDriverAndUntargetedVehicles(GameTestHelper helper) throws ReflectiveOperationException {
        var bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(2, 1, 2));
        var crew = helper.spawn(EntityType.HUSK, new Vec3(2, 1, 2));
        crew.setNoAi(true);
        crew.startRiding(bmp, true);
        bmp.addEnergy(bmp.maxVehicleEnergy());
        bmp.setOnGround(true);
        bmp.setAiVehicleInput(input(true, false, false, false, 0, 0));
        invoke(bmp, "tickServerMovement", new Class<?>[]{});
        helper.assertTrue(bmp.getControllingPassenger() == crew && bmp.enginePower() > 0,
                "An AI driver must retain powered control of the aligned vehicle");
        helper.assertTrue(bmp.vehicleData().defaults().seats().getFirst().enclosed(),
                "Camera alignment must preserve BMP occupant protection");
        crew.stopRiding();
        helper.assertTrue(bmp.enginePower() == 0, "An empty ground vehicle must clear AI throttle");
        for (String id : new String[]{"a10", "tom6", "hpj11", "laser_tower", "waveforce_tower", "test_wheel_vehicle"}) {
            var old = new ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity(ModEntities.BMP2.get(),
                    helper.getLevel(), Reference.id(id));
            helper.assertFalse(old.usesSwControls(), id + " must retain its existing control formulas");
        }
        var truck = helper.spawn(ModEntities.TRUCK.get(), new Vec3(2, 1, 2));
        var player = mockPlayer(helper);
        player.startRiding(truck, true);
        helper.assertFalse(truck.shouldBanPassengerHand(player), "Truck seats must retain handheld items");
        helper.assertFalse(truck.canPassengerUseSelectedVehicleWeapon(player), "Truck must not acquire a synthetic mounted weapon");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swAccelerationReverseAndBraking(GameTestHelper helper) throws ReflectiveOperationException {
        for (String id : new String[]{"bmp2", "lav150", "truck"}) {
            VehicleEntity vehicle = isolatedVehicle(helper, ModEntities.BMP2.get(), id);
            EngineInfo engine = vehicle.vehicleData().defaults().engine();
            double power = 0, speed = 0;
            for (int tick = 0; tick < 100; tick++) {
                boolean forward = tick < 40, reverse = tick >= 40 && tick < 80, brake = tick >= 90;
                set(vehicle, "input", input(forward, reverse, brake, false, 0, 0));
                vehicle.setXRot(0); vehicle.setYRot(0); set(vehicle, "swVelocity", 0.0D);
                vehicle.setOnGround(true);
                // Fixed SW 0cfd00d5 track/wheel reference, flat ground without collision.
                if (forward) power = Math.min(power + engine.increment() * (power < 0 ? 2 : 1), 1);
                if (reverse) power = Math.max(power - engine.decrement() * (power > 0 ? 2 : 1), -1);
                double rate = power > 0 ? engine.maxForwardSpeed() : engine.maxReverseSpeed();
                if (!forward && !reverse) power *= id.equals("bmp2") ? .96F : .97F;
                if (brake) power *= .6F;
                speed = speed * 1.05D * (.54F + .25F) + .15D * rate * power;
                invoke(vehicle,"tickSwSurfaceMovement",new Class<?>[]{EngineInfo.class,boolean.class,boolean.class},engine,false,false);
                helper.assertTrue(Math.abs(vehicle.getDeltaMovement().z - speed) <= Math.max(1.0E-6D, Math.abs(speed) * .01D),
                        id + " SW speed curve differs at tick " + tick);
                helper.assertTrue(Math.abs(vehicle.enginePower() - power) < 1.0E-6D,id + " SW power curve differs at tick " + tick);
            }
            helper.assertTrue(speed < 0, id + " must reverse after the opposite throttle sequence");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swHelicopterHoverAndPowerFloor(GameTestHelper helper) throws ReflectiveOperationException {
        var toggle = new VehicleInput(false,false,false,false,false,false,false,false,false,false,false,false,-1,false,false,0,0,false,true);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var payload = new ttv.migami.jeg.vehicle.network.VehicleInputPayload(42,false,false,false,false,false,false,false,false,false,false,false,false,-1,false,false,0,0,false,true);
            ttv.migami.jeg.vehicle.network.VehicleInputPayload.STREAM_CODEC.encode(buffer, payload);
            var decoded = ttv.migami.jeg.vehicle.network.VehicleInputPayload.STREAM_CODEC.decode(buffer);
            helper.assertTrue(payload.equals(decoded) && toggle.equals(decoded.toInput()) && !buffer.isReadable(), "Hover toggle must survive the network codec and input conversion");
        } finally { buffer.release(); }
        for (String id : new String[]{"mi28", "ah6"}) {
            VehicleEntity vehicle = isolatedVehicle(helper, ModEntities.AH6.get(), id);
            EngineInfo engine = vehicle.vehicleData().defaults().engine();
            vehicle.primeAiHelicopterSpawnHover();
            vehicle.setOnGround(false);
            vehicle.setXRot(15); invoke(vehicle,"setRoll",new Class<?>[]{float.class},12F);
            vehicle.setDeltaMovement(.4, -.15, .2);
            invoke(vehicle,"applyDriverInput",new Class<?>[]{VehicleInput.class},toggle);
            helper.assertTrue(vehicle.hoverMode(), id + " must enable hover in powered flight");
            set(vehicle,"input",VehicleInput.EMPTY);
            for (int tick = 0; tick < 400; tick++) {
                vehicle.setOnGround(false);
                invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
            }
            System.out.println("JEG hover " + id + ": pitch=" + vehicle.getXRot() + " roll=" + vehicle.roll() + " velocity=" + vehicle.getDeltaMovement());
            helper.assertTrue(Math.abs(vehicle.getXRot()) < 3 && Math.abs(vehicle.roll()) < 3, id + " hover must level pitch and roll");
            helper.assertTrue(vehicle.getDeltaMovement().horizontalDistance() < .03 && Math.abs(vehicle.getDeltaMovement().y) < .03, id + " hover must settle drift and vertical speed");
            invoke(vehicle,"applyDriverInput",new Class<?>[]{VehicleInput.class},toggle);
            helper.assertFalse(vehicle.hoverMode(), "A second click must disable hover");
            invoke(vehicle,"applyDriverInput",new Class<?>[]{VehicleInput.class},toggle);
            vehicle.setOnGround(true);
            invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
            helper.assertFalse(vehicle.hoverMode(), "Landing must cancel hover");
            invoke(vehicle,"applyDriverInput",new Class<?>[]{VehicleInput.class},toggle);
            helper.assertFalse(vehicle.hoverMode(), "Hover must remain unavailable on the ground");
            vehicle.setOnGround(false);
            invoke(vehicle,"applyDriverInput",new Class<?>[]{VehicleInput.class},toggle);
            vehicle.consumeEnergy(vehicle.vehicleEnergy());
            invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
            helper.assertFalse(vehicle.hoverMode(), "Loss of energy must cancel hover");
            vehicle.addEnergy(vehicle.maxVehicleEnergy()); vehicle.primeAiHelicopterSpawnHover();
            set(vehicle,"enginePower",.08D);
            set(vehicle,"input",new VehicleInput(false,false,false,false,true,false,true,false,false,false,false,false,-1,false,false,0,0));
            for (int tick=0;tick<60;tick++) invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
            helper.assertTrue(Math.abs(vehicle.enginePower() - .0225F / engine.liftSpeed()) < 1.0E-6, "Manual descent must use the slightly lower airborne power floor");
            set(vehicle,"input",input(false,true,false,false,0,0));
            set(vehicle,"enginePower",.08D);
            for (int tick=0;tick<60;tick++) invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
            helper.assertTrue(Math.abs(vehicle.enginePower() - .055F / engine.liftSpeed()) < 1.0E-6, "Landing assist must retain a separate safe power floor");
        }
        var mounted = helper.spawn(ModEntities.MI28.get(), new Vec3(2,20,2));
        net.minecraft.server.level.ServerPlayer driver=mockPlayer(helper), passenger=mockPlayer(helper), outsider=mockPlayer(helper);
        driver.startRiding(mounted,true); passenger.startRiding(mounted,true);
        mounted.primeAiHelicopterSpawnHover(); mounted.setOnGround(false);
        mounted.processInput(passenger,toggle); mounted.processInput(outsider,toggle);
        helper.assertFalse(mounted.hoverMode(), "Passengers and outsiders must not toggle pilot hover");
        mounted.processInput(driver,toggle);
        helper.assertTrue(mounted.hoverMode(), "The mounted driver must control hover");
        driver.stopRiding();
        helper.assertFalse(mounted.hoverMode(), "Dismounting must clear hover");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swHelicopterStartupAndLift(GameTestHelper helper) throws ReflectiveOperationException {
        for (String id : new String[]{"mi28", "ah6"}) {
            VehicleEntity vehicle = isolatedVehicle(helper, ModEntities.AH6.get(), id);
            EngineInfo engine = vehicle.vehicleData().defaults().engine();
            double power = 0, rotor = 0, vertical = 0;
            int hold = 0;
            boolean ready = false;
            set(vehicle,"input",input(true,false,false,false,0,0));
            for (int tick = 0; tick < 80; tick++) {
                vehicle.setOnGround(false);
                if (ready) power = Math.min(power + .0007F * engine.increment() * Math.min(++hold,10),.12F);
                else power = Math.min(power + .0012F * engine.increment(),.045F);
                rotor = (.82F * rotor + .18F * (float)power) * .9995F;
                vertical = vertical * .95D + rotor * engine.liftSpeed() - .06D;
                ready |= power > .04F;
                invoke(vehicle,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},engine,false);
                helper.assertTrue(Math.abs(vehicle.getDeltaMovement().y - vertical) <= Math.max(1.0E-5D,Math.abs(vertical)*.01D),
                        id + " SW lift curve differs at tick " + tick);
            }
            helper.assertTrue(vehicle.engineReady() && vehicle.getDeltaMovement().y > 0,id + " must finish startup and climb");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void seatPermissionsTurretAndInventoryRoundTrip(GameTestHelper helper) throws ReflectiveOperationException {
        var bmp = helper.spawn(ModEntities.BMP2.get(), new Vec3(2,1,2));
        var player = mockPlayer(helper);
        player.startRiding(bmp,true);
        helper.assertTrue(bmp.canPassengerUseVehicleWeapon(player,0),"BMP driver must own the cannon");
        helper.assertFalse(bmp.canPassengerUseVehicleWeapon(player,3),"BMP driver must not own another seat's machine gun");
        player.setXRot(-90);
        bmp.processInput(player,input(false,false,false,false,0,0));
        for(int tick=0;tick<20;tick++) invoke(bmp,"tickSwTurret",new Class<?>[]{});
        helper.assertTrue(Math.abs(bmp.turretPitch()+74)<.1F,"BMP cannon must elevate to SW's 74 degree limit");
        bmp.processInput(player,new VehicleInput(false,false,false,false,false,false,false,false,false,false,false,false,-1,false,false,0,0,true));
        helper.assertTrue(bmp.isPassengerAiming(player),"The driver aiming state must replicate");
        helper.assertFalse(bmp.isPassengerAiming(mockPlayer(helper)),"A non-passenger must not inherit a seat's aiming state");
        bmp.changeSeat(player);
        helper.assertFalse(bmp.isPassengerAiming(player),"Changing seat must immediately clear old aiming state");
        helper.assertTrue(bmp.getSeatIndex(player)==1 && bmp.canPassengerUseVehicleWeapon(player,3),
                "Changing seat must transfer control to that seat's machine gun");
        helper.assertFalse(bmp.canPassengerUseVehicleWeapon(player,0),"A former driver must lose cannon permission");
        bmp.vehicleInventory().setItem(101,new ItemStack(Items.DIAMOND,7));
        var saved=bmp.saveVehicleContainerState();
        var restored=helper.spawn(ModEntities.BMP2.get(),new Vec3(2,1,2));
        restored.loadVehicleContainerState(saved);
        helper.assertTrue(restored.vehicleInventory().getItem(101).getCount()==7,"Changing exposed slots must preserve hidden stored items");
        var outsider=mockPlayer(helper);
        double power=bmp.enginePower();
        bmp.processInput(outsider,input(true,false,false,false,100,100));
        helper.assertTrue(bmp.enginePower()==power,"A non-passenger must not control the vehicle");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swSteeringAndShipBuoyancy(GameTestHelper helper) throws ReflectiveOperationException {
        for(String id:new String[]{"bmp2","lav150","truck","speedboat"}) {
            VehicleEntity vehicle=isolatedVehicle(helper,ModEntities.BMP2.get(),id);
            EngineInfo engine=vehicle.vehicleData().defaults().engine();
            boolean track=id.equals("bmp2"),ship=id.equals("speedboat");
            Vec3 velocity=Vec3.ZERO;
            double power=0,steering=0,rudder=0;
            float yaw=0;
            for(int tick=0;tick<80;tick++) {
                boolean forward=tick<50,reverse=tick>=50&&tick<70,right=tick<70;
                vehicle.setXRot(0);vehicle.setYRot(yaw);vehicle.setOnGround(!ship);
                set(vehicle,"input",input(forward,reverse,false,right,0,0));
                Vec3 view=Vec3.directionFromRotation(0,yaw);
                if(ship) velocity=velocity.add(0,engine.buoyancy(),0);
                double dot=velocity.multiply(1,0,1).normalize().dot(view.multiply(1,0,1).normalize());
                double direct=(90-Math.toDegrees(Math.acos(net.minecraft.util.Mth.clamp(dot,-1,1))))/90;
                double drag=ship?.75F-.04F+.09F*Math.abs(direct):.54F+.25F*Math.abs(direct);
                velocity=velocity.add(view.scale((ship?.04D:.05D)*velocity.dot(view))).multiply(drag,ship?.85D:.99D,drag);
                if(forward) power=Math.min(power+engine.increment()*(power<0?2:1),1);
                if(reverse) power=Math.max(power-engine.decrement()*(power>0?2:1),-1);
                double rate=power>0?engine.maxForwardSpeed():engine.maxReverseSpeed();
                if(!forward&&!reverse) power*=track?.96F:.97F;
                if(right) power*=track?.96F:.98F;
                if(right) steering+=engine.steeringSpeed()*(ship||track&&!reverse?-1:1);
                steering*=track?Math.max(.76F-.1F*velocity.horizontalDistance(),.3D):Math.max(.78F-.25F*velocity.horizontalDistance(),.1D);
                rudder=net.minecraft.util.Mth.clamp(rudder-steering,-.8D,.8D)*.75F;
                float pitch=ship?(float)(-direct*engine.bodyPitchRate()*velocity.horizontalDistance()):0;
                yaw-=ship?20*velocity.horizontalDistance()*steering*(power>0?1:-1):track?6*steering:12*velocity.horizontalDistance()*rudder*(power>0?1:-1);
                velocity=velocity.add(Vec3.directionFromRotation(pitch,yaw).scale(.15D*rate*power)).add(0,-.06D,0);
                invoke(vehicle,"tickSwSurfaceMovement",new Class<?>[]{EngineInfo.class,boolean.class,boolean.class},engine,ship,false);
                helper.assertTrue(vehicle.getDeltaMovement().distanceTo(velocity)<=Math.max(1.0E-5D,velocity.length()*.01D),id+" SW steering curve differs at "+tick);
                helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(vehicle.getYRot()-yaw))<.1F,id+" SW yaw differs at "+tick);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void swLandingTargetAndNoEnergy(GameTestHelper helper) throws ReflectiveOperationException {
        var heli=isolatedVehicle(helper,ModEntities.AH6.get(),"ah6");
        var pad=helper.absolutePos(new net.minecraft.core.BlockPos(2,1,2));
        helper.getLevel().setBlockAndUpdate(pad,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(Reference.id("vehicle_charging_station")).defaultBlockState());
        heli.setPos(Vec3.atCenterOf(pad).add(8,8,0));
        Method find=VehicleEntity.class.getDeclaredMethod("swLandingPosition");find.setAccessible(true);
        Vec3 landing=(Vec3)find.invoke(heli);
        helper.assertTrue(landing!=null&&landing.distanceTo(Vec3.atCenterOf(pad))<.01,"Automatic landing must find the tagged pad below");
        set(heli,"enginePower",.08D);set(heli,"engineStartOver",true);set(heli,"engineStart",true);
        set(heli,"input",input(false,true,false,false,0,0));
        invoke(heli,"tickSwHelicopterMovement",new Class<?>[]{EngineInfo.class,boolean.class},heli.vehicleData().defaults().engine(),false);
        helper.assertTrue(heli.enginePower()<.08D&&heli.roll()>0,"Landing input must reduce lift and tilt toward the pad");
        var bmp=isolatedVehicle(helper,ModEntities.BMP2.get(),"bmp2");
        bmp.consumeEnergy(bmp.vehicleEnergy());bmp.setOnGround(true);
        set(bmp,"input",input(true,false,false,true,0,0));
        invoke(bmp,"tickSwSurfaceMovement",new Class<?>[]{EngineInfo.class,boolean.class,boolean.class},bmp.vehicleData().defaults().engine(),false,false);
        helper.assertTrue(bmp.enginePower()==0&&bmp.steeringPower()==0,"Empty energy must neutralize powered track controls");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void passengerPoseAndInventoryQuickMove(GameTestHelper helper) {
        var vehicle=helper.spawn(ModEntities.BMP2.get(),new Vec3(2,1,2));
        var player=mockPlayer(helper);
        player.startRiding(vehicle,true);
        vehicle.changeSeat(player);
        vehicle.setYRot(30);vehicle.setXRot(10);
        player.setYRot(-170);player.setXRot(-80);
        vehicle.clampSwPassengerView(player);
        var seat=vehicle.vehicleData().defaults().seats().get(1);
        float relative=net.minecraft.util.Mth.wrapDegrees(player.getYRot()-vehicle.getYRot()-seat.orientation());
        helper.assertTrue(relative>=seat.minYaw()&&relative<=seat.maxYaw(),"Passenger yaw must respect seat orientation");
        helper.assertTrue(Math.abs(player.yBodyRot-(vehicle.getYRot()+seat.orientation()))<.1,"Seated body must follow the seat instead of the viewing direction");
        vehicle.vehicleInventory().setItem(0,new ItemStack(Items.IRON_INGOT,16));
        vehicle.vehicleInventory().setItem(101,new ItemStack(Items.DIAMOND,3));
        var menu=new ttv.migami.jeg.vehicle.menu.VehicleMenu(1,player.getInventory(),vehicle.vehicleInventory(),54,vehicle);
        helper.assertTrue(menu.quickMoveStack(player,0).getCount()==16&&vehicle.vehicleInventory().getItem(0).isEmpty(),"Quick move must transfer the vehicle stack");
        menu.quickMoveStack(player,54+27+8);
        helper.assertTrue(vehicle.vehicleInventory().getItem(0).getCount()==16,"Quick move must return the stack to the visible vehicle slots");
        helper.assertTrue(vehicle.vehicleInventory().getItem(101).getCount()==3,"The smaller SW menu must preserve old hidden slots");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cameraTransformsAndInputSanitization(GameTestHelper helper) {
        var bmp=helper.spawn(ModEntities.BMP2.get(),new Vec3(2,1,2));
        bmp.setYRot(0); bmp.yRotO=0; bmp.setXRot(0); bmp.xRotO=0;
        var turret=bmp.vehicleData().defaults().turret();
        Vec3 position=bmp.swPosition("barrel",new Vec3(-.4,.4,0),1);
        Vec3 expected=bmp.position().add(turret.originX()+turret.barrelX()-.4,turret.originY()+turret.barrelY()+.4,turret.originZ()+turret.barrelZ());
        helper.assertTrue(position.distanceTo(expected)<.01,"BMP sight transform must agree within one centimeter");
        var mi=helper.spawn(ModEntities.MI28.get(),new Vec3(2,1,2));
        var camera=mi.vehicleData().defaults().seats().get(1).zoomCamera();
        helper.assertTrue(camera.hasZoomPosition() && Math.abs(camera.y()-2.9375)<1.0E-8 && Math.abs(camera.zoomY()-1.0625)<1.0E-8,
                "Mi-28 gunner must keep separate normal and aiming camera positions");
        mi.syncAuthoritativeControls(0, 0, 0, 18, false, false);
        mi.syncAuthoritativeState(mi.getX(), mi.getY(), mi.getZ(), 0, 0, 0, 25, 12, true);
        helper.assertTrue(Math.abs(mi.roll(.25F)-18)<.001F,
                "Forced position correction must also snap the previous roll used by the camera");
        VehicleInput invalid=input(false,false,false,false,Float.NaN,Float.POSITIVE_INFINITY);
        helper.assertTrue(invalid.mouseX()==0 && invalid.mouseY()==0,"Non-finite network mouse input must be neutralized");
        var pilot = mockPlayer(helper);
        pilot.startRiding(mi, true);
        mi.setYRot(0); mi.yRotO = 0; mi.setXRot(0); mi.xRotO = 0;
        mi.setPos(mi.position().add(0, 50, 0));
        mi.xo = mi.getX(); mi.yo = mi.getY(); mi.zo = mi.getZ();
        Vec3 origin = mi.position().add(0, mi.rotateOffsetHeight(), 0);
        Vec3 openCamera = mi.swAircraftCameraPosition(pilot, 1, 0, 0, 0);
        var obstruction = net.minecraft.core.BlockPos.containing(origin.lerp(openCamera, .5));
        helper.getLevel().setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        Vec3 clippedCamera = mi.swAircraftCameraPosition(pilot, 1, 0, 0, 0);
        helper.assertTrue(clippedCamera.distanceTo(origin) < openCamera.distanceTo(origin) - 1,
                "Aircraft camera must move in front of a wall: open=" + openCamera + ", clipped=" + clippedCamera);
        helper.getLevel().setBlockAndUpdate(obstruction, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void gunnerHudAndActualHitFeedback(GameTestHelper helper) {
        var received = new java.util.concurrent.atomic.AtomicReference<ttv.migami.jeg.network.HitMarkerPayload>();
        var player = mockPlayer(helper, packet -> {
            if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom && custom.payload() instanceof ttv.migami.jeg.network.HitMarkerPayload marker) received.set(marker);
        });
        var mi = helper.spawn(ModEntities.MI28.get(), new Vec3(2,1,2));
        player.startRiding(mi, true); mi.changeSeat(player);
        int slot = mi.vehicleWeaponIndexForDisplaySlot(player, 1);
        mi.processInput(player, new VehicleInput(false,false,false,false,false,false,false,false,false,false,false,false,slot,false,false,0,0));
        mi.setXRot(12); mi.xRotO = 12; mi.syncAuthoritativeControls(0,0,0,18,false,false); mi.snapControlInterpolation();
        Vec3 expected = mi.swPosition("barrel", new Vec3(0,0,1), 1).subtract(mi.swPosition("barrel", Vec3.ZERO, 1)).normalize();
        helper.assertTrue(mi.vehicleHudShootDirection(player,1).distanceTo(expected) < .000001, "Mi gunner missile HUD must inherit its barrel view direction");
        var target = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new Vec3(5,1,5));
        var source = target.damageSources().thrown(player, player);
        ttv.migami.jeg.init.ModDamageTypes.hurtWithPlayerKillCredit(target, source, 1, player);
        helper.assertTrue(received.get() != null && !received.get().vehicle() && !received.get().killed(), "A successful gunner hit must send live hit feedback");
        received.set(null); target.invulnerableTime = 0;
        ttv.migami.jeg.init.ModDamageTypes.hurtWithPlayerKillCredit(target, source, 100, player);
        helper.assertTrue(received.get() != null && received.get().killed(), "Kill feedback must follow the actual damage result");
        player.stopRiding(); received.set(null);
        ttv.migami.jeg.advancement.GameplayActions.hit(player, player, target, false);
        helper.assertTrue(received.get() == null, "Unmounted players must not receive mounted weapon feedback");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var expectedPayload = new ttv.migami.jeg.network.HitMarkerPayload(true, true, true);
            ttv.migami.jeg.network.HitMarkerPayload.STREAM_CODEC.encode(buffer, expectedPayload);
            helper.assertTrue(expectedPayload.equals(ttv.migami.jeg.network.HitMarkerPayload.STREAM_CODEC.decode(buffer)),
                    "Critical, vehicle and kill feedback flags must survive the existing network codec");
        } finally { buffer.release(); }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void enemySurfaceRetreatTurningAndIdle(GameTestHelper helper) throws ReflectiveOperationException {
        var brain = enemyBrain();
        var vehicle = isolatedVehicle(helper, ModEntities.BMP2.get(), "bmp2");
        vehicle.setPos(helper.absoluteVec(new Vec3(2, 20, 2)));
        Vec3 target = vehicle.position().add(-10, 0, 10);
        enemyCall("driveToward", vehicle, brain, target, 180D, 5D, true);
        VehicleInput retreat = aiInput(vehicle);
        helper.assertTrue(retreat.backward() && retreat.left() && !retreat.brake(),
                "Close enemies must retreat and invert steering when reversing");
        enemyCall("driveToward", vehicle, brain, vehicle.position().add(-30, 0, 0), 5D, 5D, false);
        helper.assertTrue(!aiInput(vehicle).forward() && aiInput(vehicle).right(),
                "BMP must pivot before accelerating into a sharp turn");
        for (int tick = 0; tick < 100; tick++) {
            vehicle.tickCount = tick;
            enemyCall("driveToward", vehicle, brain, vehicle.position(), 12D, 8D, false);
            helper.assertTrue(aiInput(vehicle).brake() && !aiInput(vehicle).backward(),
                    "An idle patrol must not classify its deliberate stop as being stuck");
        }
        var truck = isolatedVehicle(helper, ModEntities.TRUCK.get(), "truck");
        truck.setPos(vehicle.position());
        enemyCall("driveToward", truck, enemyBrain(), truck.position().add(0, 0, 30), 5D, 5D, false);
        helper.assertTrue(aiInput(truck).forward(), "Truck AI must retain ordinary pursuit control");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void enemyBoatWaterAndShoreAvoidance(GameTestHelper helper) throws ReflectiveOperationException {
        var boat = isolatedVehicle(helper, ModEntities.SPEEDBOAT.get(), "speedboat");
        boat.setPos(helper.absoluteVec(new Vec3(502, 30, 502)));
        var level = helper.getLevel();
        var samples = new java.util.HashMap<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (int step = 3; step <= 8; step++) {
            var pos = net.minecraft.core.BlockPos.containing(boat.position().add(0, .1, step));
            samples.put(pos, level.getBlockState(pos)); level.setBlock(pos, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
        }
        try {
            Method unsafe = EnemyVehicleController.class.getDeclaredMethod("isForwardUnsafe", VehicleEntity.class, boolean.class);
            unsafe.setAccessible(true);
            helper.assertFalse((boolean) unsafe.invoke(null, boat, false), "A boat must accept open water as its route");
            var truck = isolatedVehicle(helper, ModEntities.TRUCK.get(), "truck"); truck.setPos(boat.position());
            helper.assertTrue((boolean) unsafe.invoke(null, truck, false), "A truck must avoid the same water route");
            level.setBlock(net.minecraft.core.BlockPos.containing(boat.position().add(0, .1, 8)), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue((boolean) unsafe.invoke(null, boat, false), "A boat must detect an approaching shoreline");
        } finally {
            samples.forEach((pos, state) -> level.setBlock(pos, state, 3));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void enemyTurretSlewAndActualWeaponAlignment(GameTestHelper helper) throws ReflectiveOperationException {
        for (var type : new EntityType[]{ModEntities.BMP2.get(), ModEntities.LAV150.get(), ModEntities.SPEEDBOAT.get(), ModEntities.MI28.get()}) {
            VehicleEntity vehicle = (VehicleEntity) helper.spawn(type, new Vec3(2, 20, 2));
            var crew = helper.spawn(EntityType.HUSK, new Vec3(2, 20, 2));
            crew.setNoAi(true);
            int seat = vehicle.vehicleData().defaults().turret().seatIndex();
            vehicle.rememberSeatAssignment(crew, seat); crew.startRiding(vehicle, true);
            vehicle.selectAiWeaponForSeat(seat, seat == 1 ? 3 : 0);
            vehicle.setAiTurretAim(90, -90);
            helper.assertTrue(vehicle.turretYaw() == 0 && vehicle.turretPitch() == 0,
                    "Enemy turret aim must not teleport past SW turn rates");
            invoke(vehicle, "tickSwTurret", new Class<?>[]{});
            helper.assertTrue(Math.abs(vehicle.turretYaw()) <= vehicle.vehicleData().defaults().turret().yawTurnSpeed(),
                    "Enemy yaw slew must use the configured turret rate");
            for (int tick = 0; tick < 60; tick++) invoke(vehicle, "tickSwTurret", new Class<?>[]{});
            helper.assertTrue(Math.abs(vehicle.turretPitch() + vehicle.vehicleData().defaults().turret().maxPitch()) < .1,
                    "Enemy elevation must honor the matching vehicle's SW pitch limit");
            Vec3 muzzle = vehicle.aiWeaponMuzzlePosition(vehicle.selectedVehicleWeaponIndex(crew));
            Vec3 direction = vehicle.vehicleHudShootDirection(crew, 1);
            Method actualMuzzle = VehicleEntity.class.getDeclaredMethod("weaponMuzzlePosition", ttv.migami.jeg.vehicle.data.subdata.VehicleWeaponInfo.class,
                    Vec3.class, double.class, double.class); actualMuzzle.setAccessible(true);
            var weapon = vehicle.vehicleData().defaults().weapons().get(vehicle.selectedVehicleWeaponIndex(crew));
            helper.assertTrue(muzzle.distanceTo((Vec3) actualMuzzle.invoke(vehicle, weapon, direction, 1.25D, .95D)) < 1.0E-6,
                    "Enemy aim must originate at the same muzzle that actually launches the projectile");
            Method aligned = EnemyVehicleController.class.getDeclaredMethod("weaponAligned", VehicleEntity.class, LivingEntity.class, Vec3.class, double.class);
            aligned.setAccessible(true);
            helper.assertTrue((boolean) aligned.invoke(null, vehicle, crew, muzzle.add(direction.scale(40)), 5D),
                    "A target on the real weapon axis must be eligible for fire");
            helper.assertFalse((boolean) aligned.invoke(null, vehicle, crew, muzzle.subtract(direction.scale(40)), 5D),
                    "AI must not fire at a target behind its actual barrel");
            vehicle.discard(); crew.discard();
        }
        var ah = helper.spawn(ModEntities.AH6.get(), new Vec3(2, 20, 2));
        var pilot = helper.spawn(EntityType.HUSK, new Vec3(2, 20, 2));
        pilot.setNoAi(true); pilot.startRiding(ah, true); ah.selectAiWeaponForSeat(0, 0);
        var target = mockPlayer(helper);
        Vec3 point = ah.aiWeaponMuzzlePosition(0).add(Vec3.directionFromRotation(10, 0).scale(60));
        target.setPos(point.x, point.y - target.getEyeHeight(), point.z);
        var aimConstructor = Class.forName(EnemyVehicleController.class.getName() + "$Aim").getDeclaredConstructor(float.class, float.class, float.class);
        aimConstructor.setAccessible(true);
        enemyCall("tickAh6Weapons", ah, pilot, target, aimConstructor.newInstance(0F, 0F, 10F), 60D, true);
        helper.assertTrue(ah.selectedVehicleWeaponIndex(pilot) == 0,
                "AH-6 must use its machine gun when the nose angle permits gun fire but not an accurate rocket");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void enemySwFlightClosedLoop(GameTestHelper helper) throws ReflectiveOperationException {
        for (String id : new String[]{"mi28", "ah6"}) {
            var pilot = mockPlayer(helper);
            VehicleEntity vehicle = new VehicleEntity(ModEntities.AH6.get(), helper.getLevel()) {
                { setVehicleData(Reference.id(id)); addEnergy(maxVehicleEnergy()); }
                @Override public LivingEntity getControllingPassenger() { return pilot; }
                @Override public void move(MoverType mover, Vec3 delta) { setPos(position().add(delta)); setDeltaMovement(delta); }
            };
            Vec3 origin = helper.absoluteVec(new Vec3(1002, 1, 1002));
            helper.getLevel().getChunkAt(net.minecraft.core.BlockPos.containing(origin));
            int ground = helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) origin.x, (int) origin.z);
            vehicle.setPos(origin.x, ground + 12, origin.z); vehicle.setYRot(90);
            vehicle.primeAiHelicopterSpawnHover();
            var brain = enemyBrain();
            Vec3 destination = vehicle.position().add(0, 0, 45);
            double peakRoll = 0, peakSpeed = 0;
            for (int tick = 0; tick < 400; tick++) {
                vehicle.tickCount = tick; vehicle.xo = vehicle.getX(); vehicle.yo = vehicle.getY(); vehicle.zo = vehicle.getZ();
                vehicle.yRotO = vehicle.getYRot(); vehicle.xRotO = vehicle.getXRot();
                enemyCall("flySwToward", vehicle, brain, destination, null, 24D, id);
                VehicleInput input = aiInput(vehicle);
                helper.assertFalse(input.forward() || input.backward(), "SW AI must use tilt and collective, without auto landing input");
                invoke(vehicle, "tickSwHelicopterMovement", new Class<?>[]{EngineInfo.class, boolean.class}, vehicle.vehicleData().defaults().engine(), false);
                peakRoll = Math.max(peakRoll, Math.abs(vehicle.roll())); peakSpeed = Math.max(peakSpeed, vehicle.getDeltaMovement().horizontalDistance());
            }
            double altitude = vehicle.getY() - helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.util.Mth.floor(vehicle.getX()), net.minecraft.util.Mth.floor(vehicle.getZ()));
            System.out.println("JEG enemy flight " + id + ": altitude=" + altitude + " distance=" + destination.subtract(vehicle.position()).horizontalDistance()
                    + " peakRoll=" + peakRoll + " peakSpeed=" + peakSpeed);
            helper.assertTrue(altitude > 20 && altitude < 28, id + " must climb then hold its patrol altitude");
            helper.assertTrue(destination.subtract(vehicle.position()).horizontalDistance() < 20, id + " must close on its patrol point");
            helper.assertTrue(peakRoll < 40 && peakSpeed < 0.9, id + " must retain playable banking and speed");
            var target = mockPlayer(helper);
            target.setPos(vehicle.getX(), ground + 1, vehicle.getZ() - 10);
            enemyCall("airEngage", vehicle, brain, target, id, (double) vehicle.distanceTo(target));
            Field evasion = brain.getClass().getDeclaredField("airEvasionTicks"); evasion.setAccessible(true);
            helper.assertTrue(evasion.getInt(brain) == 0, "Close-range retreat must not indefinitely request an obstacle climb");
            double attackPeakSpeed = 0, attackPeakAltitude = 0;
            int noseAimTicks = 0;
            for (int tick = 0; tick < 600; tick++) {
                vehicle.tickCount++;
                vehicle.xo = vehicle.getX(); vehicle.yo = vehicle.getY(); vehicle.zo = vehicle.getZ();
                vehicle.yRotO = vehicle.getYRot(); vehicle.xRotO = vehicle.getXRot();
                enemyCall("airEngage", vehicle, brain, target, id, (double) vehicle.distanceTo(target));
                invoke(vehicle, "tickSwHelicopterMovement", new Class<?>[]{EngineInfo.class, boolean.class}, vehicle.vehicleData().defaults().engine(), false);
                attackPeakSpeed = Math.max(attackPeakSpeed, vehicle.getDeltaMovement().horizontalDistance());
                attackPeakAltitude = Math.max(attackPeakAltitude, vehicle.getY() - ground);
                Vec3 toTarget = target.getEyePosition().subtract(vehicle.position());
                if (vehicle.getViewVector(1).dot(toTarget.normalize()) > Math.cos(Math.toRadians(12))) noseAimTicks++;
            }
            System.out.println("JEG enemy attack " + id + ": peakSpeed=" + attackPeakSpeed + " peakAltitude=" + attackPeakAltitude + " noseAimTicks=" + noseAimTicks);
            helper.assertTrue(attackPeakSpeed < 1.05 && attackPeakAltitude < 60, id + " must keep attack runs within playable speed and altitude");
            helper.assertTrue(noseAimTicks > 5, id + " must bring its fixed nose weapons onto the target during attack runs");
        }
        helper.succeed();
    }

    private static Object enemyBrain() throws ReflectiveOperationException {
        var constructor = Class.forName(EnemyVehicleController.class.getName() + "$Brain").getDeclaredConstructor();
        constructor.setAccessible(true); return constructor.newInstance();
    }

    private static void enemyCall(String name, Object... arguments) throws ReflectiveOperationException {
        Method method = java.util.Arrays.stream(EnemyVehicleController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
        method.setAccessible(true); method.invoke(null, arguments);
    }

    private static VehicleInput aiInput(VehicleEntity vehicle) throws ReflectiveOperationException {
        Field field = VehicleEntity.class.getDeclaredField("input"); field.setAccessible(true); return (VehicleInput) field.get(vehicle);
    }

    private static net.minecraft.server.level.ServerPlayer mockPlayer(GameTestHelper helper) {
        return mockPlayer(helper, packet -> {});
    }

    private static net.minecraft.server.level.ServerPlayer mockPlayer(GameTestHelper helper, java.util.function.Consumer<net.minecraft.network.protocol.Packet<?>> receive) {
        var player=new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "vehicle-test"), net.minecraft.server.level.ClientInformation.createDefault());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { receive.accept(packet); }
            @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation channel) { return false; }
        };
        return player;
    }

    private static VehicleEntity isolatedVehicle(GameTestHelper helper, EntityType<? extends VehicleEntity> type, String id) {
        LivingEntity pilot=mockPlayer(helper);
        return new VehicleEntity(type,helper.getLevel()) {
            { this.setVehicleData(Reference.id(id)); this.addEnergy(this.maxVehicleEnergy()); }
            @Override public LivingEntity getControllingPassenger() { return pilot; }
            @Override public void move(MoverType mover,Vec3 delta) { this.setDeltaMovement(delta); }
            @Override public double getFluidHeight(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag) {
                return id.equals("speedboat") ? 1.0D : 0.0D;
            }
        };
    }

    private static VehicleInput input(boolean forward,boolean reverse,boolean brake,boolean right,float x,float y) {
        return new VehicleInput(forward,reverse,false,right,brake,false,false,false,false,false,false,false,-1,false,false,x,y);
    }
    private static void set(VehicleEntity vehicle,String name,Object value) throws ReflectiveOperationException {
        Field field=VehicleEntity.class.getDeclaredField(name);field.setAccessible(true);field.set(vehicle,value);
    }
    private static void invoke(VehicleEntity vehicle,String name,Class<?>[] types,Object...args) throws ReflectiveOperationException {
        Method method=VehicleEntity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(vehicle,args);
    }
}
