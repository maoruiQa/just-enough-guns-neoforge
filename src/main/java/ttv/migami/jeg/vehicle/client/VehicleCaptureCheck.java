package ttv.migami.jeg.vehicle.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/** Opt-in real-render smoke check. Run only in a disposable creative test world. */
public final class VehicleCaptureCheck {
    private static final String OUTPUT = System.getProperty("jeg.vehicleCapture", "");
    private static final boolean HANDHELD = Boolean.getBoolean("jeg.vehicleCaptureHandheld");
    private static final boolean HUD_ONLY = Boolean.getBoolean("jeg.vehicleCaptureHudOnly");
    private static final boolean SURVIVAL = Boolean.getBoolean("jeg.vehicleCaptureSurvival");
    private static final String[] VEHICLES = HANDHELD ? new String[]{"truck"} : new String[]{"bmp2", "lav150", "truck", "speedboat", "mi28", "ah6"};
    private static int vehicleIndex, seat, view, stationWeapon, captures, stateTicks, entityId = -1;
    private static final boolean TILT = Boolean.getBoolean("jeg.vehicleCaptureTilt");
    private static boolean started, finished;
    private static final boolean DRIVE = "drive".equals(System.getProperty("jeg.vehicleCaptureMode"));
    private static int driveTick, lastFrameTick = -1;
    private static boolean driveReady;
    private static CameraType originalCamera;
    private static VehicleEntity fixture;
    private static net.minecraft.world.entity.monster.zombie.Husk fixturePilot;

    private VehicleCaptureCheck() {}

    /** Returns true while the fixture owns input; ordinary play never enters this path. */
    public static boolean tick(Minecraft mc) {
        if (finished && Boolean.getBoolean("jeg.vehicleCaptureExit")) mc.stop();
        if (OUTPUT.isEmpty() || finished || mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return false;
        if (DRIVE) return drive(mc);
        if (!started) {
            if (mc.gui.screen() != null) return false;
            started = true;
            originalCamera = mc.options.getCameraType();
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                if (SURVIVAL) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode survival @a");
            });
        }
        if (view == 6) {
            if (stateTicks == 0) {
                mc.options.keyUse.setDown(false);
                ttv.migami.jeg.network.NetworkHandler.sendVehicleOpenMenu(entityId);
                stateTicks = 1;
                return true;
            }
            if (++stateTicks > 200) throw new IllegalStateException("Vehicle inventory did not open: " + VEHICLES[vehicleIndex]);
            if (mc.gui.screen() == null || stateTicks < 16) return true;
            capture(mc, (VehicleEntity) mc.player.getVehicle(), false);
            mc.player.closeContainer();
            mc.gui.setScreen(null);
            view = stateTicks = 0;
            seat = 1;
            return true;
        }
        if (stateTicks == 0) prepare(mc);
        mc.gui.hud.getChat().clearMessages(false);
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle) || vehicle.getId() != entityId || vehicle.getSeatIndex(mc.player) != seat) return true;
        if (stateTicks == 1 && seat == 0 && view == 0 && stationWeapon == 0) checkCameraControls(vehicle);
        freezePose(vehicle);
        mc.getSingleplayerServer().execute(() -> { if (fixture != null) freezePose(fixture); });
        mc.options.setCameraType(CameraType.values()[view / 2]);
        mc.options.keyUse.setDown(HANDHELD && (view & 1) != 0);
        boolean aiming = (view & 1) != 0 && vehicle.canPassengerUseSelectedVehicleWeapon(mc.player);
        VehicleClientState.update(vehicle, false, aiming, false);
        mc.player.setYRot(vehicle.getYRot() + vehicle.vehicleData().defaults().seats().get(seat).orientation());
        mc.player.setXRot(0);
        mc.player.yRotO = mc.player.getYRot();
        mc.player.xRotO = 0;
        if (++stateTicks < (HANDHELD ? 40 : 16)) return true;
        if (HANDHELD) VehicleInputHandler.checkHandheldCapture(mc, (view & 1) != 0);
        capture(mc, vehicle, aiming);
        stateTicks = 0;
        if (++view == 6) {
            view = 0;
            if (++stationWeapon < Math.max(1, stationWeapons(vehicle, seat).length)) return true;
            stationWeapon = 0;
            if (seat == 0 && !HUD_ONLY) { view = 6; return true; }
            int seats = HUD_ONLY && !VEHICLES[vehicleIndex].equals("mi28") && !VEHICLES[vehicleIndex].equals("truck") ? 1 : vehicle.vehicleData().defaults().seats().size();
            if (++seat == seats) {
                seat = 0;
                if (++vehicleIndex == VEHICLES.length) {
                    finished = true;
                    mc.options.keyUse.setDown(false);
                    mc.options.setCameraType(originalCamera);
                    VehicleClientState.clear();
                    mc.getSingleplayerServer().execute(() -> {
                        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                        if (player != null) player.stopRiding();
                        if (fixture != null) fixture.discard();
                    });
                    System.out.println("JEG vehicle capture completed: " + captures + " camera states at " + OUTPUT);
                }
            }
        }
        return true;
    }

    private static void prepare(Minecraft mc) {
        // Mark pending before the server task runs, so a slow server cannot enqueue duplicates.
        stateTicks = 1;
        var uuid = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
            if (player == null) return;
            String name = VEHICLES[vehicleIndex];
            if (fixture == null || !fixture.vehicleDataId().getPath().equals(name)) {
                player.stopRiding();
                if (fixture != null) fixture.discard();
                if (DRIVE) {
                    var server = mc.getSingleplayerServer();
                    var source = server.createCommandSourceStack();
                    for (int x = -4; x <= 4; x++) for (int z = -1; z <= 12; z++) player.level().getChunk(x, z);
                    for (int y = -60; y <= -50; y++) server.getCommands().performPrefixedCommand(source,
                            "fill -64 " + y + " -16 64 " + y + " 192 minecraft:air");
                    server.getCommands().performPrefixedCommand(source, "fill -64 -61 -16 64 -61 192 minecraft:grass_block");
                    if (name.equals("speedboat")) {
                        for (int y = -64; y <= -61; y++) server.getCommands().performPrefixedCommand(source,
                                "fill -64 " + y + " -16 64 " + y + " 192 minecraft:water");
                    }
                }
                EntityType<? extends VehicleEntity> type = switch (name) {
                    case "bmp2" -> ModEntities.BMP2.get();
                    case "lav150" -> ModEntities.LAV150.get();
                    case "truck" -> ModEntities.TRUCK.get();
                    case "speedboat" -> ModEntities.SPEEDBOAT.get();
                    case "mi28" -> ModEntities.MI28.get();
                    default -> ModEntities.AH6.get();
                };
                fixture = new ConfiguredVehicleEntity(type, player.level(), Reference.id(name));
                fixture.setPos(0, DRIVE && vehicleIndex < 4 ? -60 : 30, 0);
                fixture.setNoGravity(!DRIVE);
                fixture.setYRot(0);
                fixture.addEnergy(fixture.maxVehicleEnergy());
                player.level().addFreshEntity(fixture);
                if (DRIVE) {
                    fixturePilot = new net.minecraft.world.entity.monster.zombie.Husk(net.minecraft.world.entity.EntityTypes.HUSK, player.level());
                    fixturePilot.setNoAi(true);
                    fixturePilot.setInvulnerable(true);
                    fixturePilot.setSilent(true);
                    fixturePilot.setPos(fixture.position());
                    player.level().addFreshEntity(fixturePilot);
                    fixturePilot.startRiding(fixture, true, true);
                }
                player.startRiding(fixture, true, true);
            }
            if (!DRIVE) for (int attempts = 0; fixture.getSeatIndex(player) != seat && attempts < 8; attempts++) fixture.changeSeat(player);
            if (HANDHELD) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Reference.id("revolver"));
                if (player.getMainHandItem().getItem() != item) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
            }
            if (!DRIVE) {
                int[] slots = stationWeapons(fixture, seat);
                if (slots.length > 0) fixture.selectAiWeaponForSeat(seat, slots[stationWeapon]);
            }
            fixture.setDeltaMovement(Vec3.ZERO);
            fixture.setXRot(0);
            fixture.setYRot(0);
            entityId = fixture.getId();
        });
    }

    public static void freezeFrame(VehicleEntity vehicle) {
        if (!DRIVE && !OUTPUT.isEmpty() && started && !finished && vehicle.getId() == entityId) freezePose(vehicle);
    }

    private static boolean drive(Minecraft mc) {
        if (!started) {
            if (mc.gui.screen() != null) return false;
            started = true; originalCamera = mc.options.getCameraType();
        }
        if (!driveReady) {
            if (stateTicks == 0) {
                prepare(mc);
                mc.getSingleplayerServer().execute(() -> { driveTick = 0; driveReady = true; });
            }
            return true;
        }
        if (!(mc.player.getVehicle() instanceof VehicleEntity vehicle) || vehicle.getId() != entityId) return true;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        mc.gui.hud.getChat().clearMessages(false);
        VehicleClientState.update(vehicle, false, false, false);
        mc.player.setYRot(vehicle.getYRot()); mc.player.setXRot(0);
        int frameTick = driveTick;
        if (frameTick != lastFrameTick && frameTick % 4 == 0) {
            lastFrameTick = frameTick;
            Path output = Path.of(OUTPUT).resolve("motion");
            try { Files.createDirectories(output); }
            catch (IOException error) { throw new IllegalStateException("Cannot create motion capture directory", error); }
            Screenshot.grab(output.toFile(), VEHICLES[vehicleIndex] + "-" + String.format(java.util.Locale.ROOT,"%03d",frameTick) + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
        }
        return true;
    }

    /** Called after each integrated-server tick; input and CSV use server ticks, not render FPS. */
    public static void serverTick() {
        if (!DRIVE || !driveReady || finished || fixture == null) return;
        Minecraft mc = Minecraft.getInstance();
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) return;
        fixture.level().getEntities(fixture, fixture.getBoundingBox().inflate(4),
                entity -> entity.getVehicle() != fixture && !(entity instanceof net.minecraft.world.entity.player.Player))
                .forEach(net.minecraft.world.entity.Entity::discard);
        // Server task scheduling can spawn the fixture one tick before the capture hook.
        // Establish the same physical starting state immediately before sample zero.
        if (driveTick == 0) {
            fixture.setPos(0, vehicleIndex < 4 ? -60 : 30, 0);
            fixture.setDeltaMovement(Vec3.ZERO);
            fixture.setYRot(0); fixture.setXRot(0);
        }
        Vec3 velocity = fixture.getDeltaMovement();
        String row = VEHICLES[vehicleIndex] + "," + driveTick + "," + fixture.getX() + "," + fixture.getY() + "," + fixture.getZ() + "," + velocity.x + "," + velocity.y + "," + velocity.z + "," + fixture.getYRot() + "," + fixture.getXRot() + "," + fixture.roll() + "," + fixture.enginePower() + "," + fixture.steeringPower() + "," + fixture.propellerSpeed() + "\n";
        try {
            Files.createDirectories(Path.of(OUTPUT));
            Files.writeString(Path.of(OUTPUT).resolve("motion.csv"), row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) { throw new IllegalStateException("Cannot record vehicle controls", error); }
        boolean helicopter = vehicleIndex >= 4;
        int tick = driveTick++;
        fixture.setAiVehicleInput(new ttv.migami.jeg.vehicle.entity.base.VehicleInput(
                tick < 60 || tick >= 100 && tick < 140, tick >= 60 && tick < 100,
                false, tick >= 100 && tick < 140, tick >= 140,
                helicopter && tick < 80, helicopter && tick >= 140,
                false,false,false,false,false,-1,false,false, helicopter && tick >= 100 && tick < 140 ? 1 : 0,
                helicopter && tick >= 100 && tick < 140 ? .5F : 0));
        if (driveTick == 181) {
            driveReady = false; stateTicks = 0; lastFrameTick = -1;
            player.stopRiding(); fixture.discard(); fixture = null;
            if (fixturePilot != null) { fixturePilot.discard(); fixturePilot = null; }
            if (++vehicleIndex == VEHICLES.length) {
                finished = true; mc.options.setCameraType(originalCamera); VehicleClientState.clear();
                System.out.println("JEG vehicle control recording completed: 6 vehicles, 181 server samples each at " + OUTPUT);
            }
        }
    }

    private static int[] stationWeapons(VehicleEntity vehicle, int seat) {
        var weapons = vehicle.vehicleData().defaults().weapons();
        return !vehicle.hasVehicleWeapons() ? new int[0] : java.util.stream.IntStream.range(0, weapons.size())
                .filter(index -> weapons.get(index).usableBySeat(seat)).toArray();
    }

    private static void freezePose(VehicleEntity vehicle) {
        vehicle.setPos(0, 30, 0);
        vehicle.xo = 0; vehicle.yo = 30; vehicle.zo = 0;
        vehicle.setDeltaMovement(Vec3.ZERO);
        vehicle.setXRot(TILT ? 12 : 0); vehicle.xRotO = vehicle.getXRot();
        vehicle.setYRot(TILT ? 25 : 0); vehicle.yRotO = vehicle.getYRot();
        vehicle.syncAuthoritativeControls(0, 0, 0, TILT ? 18 : 0, false, false);
        vehicle.snapControlInterpolation();
        for (var passenger : vehicle.getPassengers()) {
            vehicle.positionRider(passenger, (entity, x, y, z) -> entity.setPos(x, y, z));
            passenger.xo = passenger.getX(); passenger.yo = passenger.getY(); passenger.zo = passenger.getZ();
        }
    }

    private static void checkCameraControls(VehicleEntity vehicle) {
        VehicleClientState.clear();
        VehicleClientState.update(vehicle, true, false, false);
        var engine = vehicle.vehicleData().defaults().engine();
        if (vehicle.usesAircraftCamera(Minecraft.getInstance().player)) {
            VehicleClientState.setMouseDelta(12, -8);
            VehicleClientState.updateAircraftMouse((float)engine.mouseSensitivity(), (float)engine.mouseSpeedX(), (float)engine.mouseSpeedY(), false, true);
            if (Math.abs(VehicleClientState.mouseLerpX()-12*engine.mouseSensitivity()*engine.mouseSpeedX()) > .000001) throw new IllegalStateException("Aircraft mouse sensitivity/smoothing differs from SW");
            VehicleClientState.updateCamera(1);
            if (Math.abs(VehicleClientState.freeYaw()) < .01 || Math.abs(VehicleClientState.freePitch()) < .01) throw new IllegalStateException("Aircraft free look did not respond");
            VehicleClientState.syncMousePosition(0, 0);
            VehicleClientState.update(vehicle, false, false, false);
            for (int i = 0; i < 80; i++) VehicleClientState.updateCamera(1);
            if (Math.abs(VehicleClientState.freeYaw()) > .001 || Math.abs(VehicleClientState.freePitch()) > .001) throw new IllegalStateException("Free look did not return to center");
        }
        for (double scroll : new double[]{-999, 999}) {
            VehicleClientState.scrollCamera(scroll);
            for (int i = 0; i < 80; i++) VehicleClientState.updateCamera(1);
            if (Math.abs(VehicleClientState.cameraDistance()-(scroll < 0 ? 8 : -3)) > .000001) throw new IllegalStateException("Camera scroll limit differs from SW");
        }
        VehicleClientState.clear();
        if (VehicleClientState.mouseLerpX() != 0 || VehicleClientState.cameraDistance() != 0 || VehicleClientState.freeLookDown() || VehicleClientState.zoomDown() || VehicleClientState.seekDown()) throw new IllegalStateException("Camera/input state survived clearing");
        System.out.println("SW camera input, scroll and reset checks passed: " + vehicle.vehicleDataId());
    }

    private static void capture(Minecraft mc, VehicleEntity vehicle, boolean aiming) {
        Path output = Path.of(OUTPUT);
        String name = view == 6 ? VEHICLES[vehicleIndex] + "-inventory" : VEHICLES[vehicleIndex] + "-seat" + seat + "-" + mc.options.getCameraType().name().toLowerCase(java.util.Locale.ROOT) + ((view & 1) != 0 ? "-aim" : "-normal") + (stationWeapon == 0 ? "" : "-weapon" + stationWeapon);
        try {
            Files.createDirectories(output);
            if (view != 6) captures++;
            Vec3 hudStart = vehicle.vehicleHudShootPos(mc.player, 1);
            Vec3 hudDirection = vehicle.vehicleHudShootDirection(mc.player, 1);
            var hudScreen = ttv.migami.jeg.client.util.ScreenProjection.worldToScreen(hudStart.add(hudDirection.scale(512)));
            Files.writeString(output.resolve("hud-projection.txt"), name + " yaw " + vehicle.turretYaw(1) + " pitch " + vehicle.turretPitch(1) + " origin " + hudStart + " dir " + hudDirection + " screen " + hudScreen + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            var camera = mc.gameRenderer.mainCamera();
            Vec3 position = camera.position();
            var rotation = camera.rotation();
            org.joml.Vector3f forward = new org.joml.Vector3f(0, 0, -1).rotate(rotation);
            if (forward.distance(camera.forwardVector()) > .0001F) throw new IllegalStateException("Camera forward vector disagrees with its rotation");
            var cachedView = camera.getViewRotationMatrix(new org.joml.Matrix4f()).getNormalizedRotation(new org.joml.Quaternionf()).conjugate();
            if (1 - Math.abs(rotation.dot(cachedView)) / Math.sqrt(rotation.lengthSquared() * cachedView.lengthSquared()) > .000001) throw new IllegalStateException("Camera view matrix cache disagrees with its rotation");
            String row = name + "," + position.x + "," + position.y + "," + position.z + "," + camera.yRot() + "," + camera.xRot() + "," + vehicle.roll() + "," + mc.getWindow().getGuiScaledWidth() + "," + mc.getWindow().getGuiScaledHeight() + "," + vehicle.getX() + "," + vehicle.getY() + "," + vehicle.getZ() + "," + vehicle.getYRot() + "," + vehicle.getXRot() + "," + aiming + "," + rotation.x + "," + rotation.y + "," + rotation.z + "," + rotation.w + "\n";
            Files.writeString(output.resolve("camera.csv"), row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Screenshot.grab(output.toFile(), name + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> System.out.println("Vehicle capture: " + message.getString()));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write vehicle capture", error);
        }
    }
}
