package ttv.migami.jeg.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.*;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.item.SpecialExplosiveItem;
import ttv.migami.jeg.vehicle.entity.ConfiguredVehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;

/** Opt-in rendered regression check. QuickPlay into a disposable creative world. */
public final class ExplosionShakeCheck {
    private static final String OUTPUT = System.getProperty("jeg.explosionShakeCheck", "");
    private static final String[] SCENES = {"tnt", "grenade", "stun", "vehicle", "missile", "c4-chain",
            "far", "third-person", "mounted", "continuous", "disabled", "excluded", "recovery"};
    private static final StringBuilder TRACE = new StringBuilder("scene,tick,notification,yaw,pitch,roll,state_tick,due_tick\n");
    private static int scene, ticks, notifications, movingFrames, originalStrength;
    private static long dueTick;
    private static boolean prepared, started, finished;
    private static int warmup;
    private static CameraType originalCamera;
    private static ConfiguredVehicleEntity mount;

    private ExplosionShakeCheck() {}

    public static void notification() {
        if (!OUTPUT.isEmpty() && started && !finished) notifications++;
    }

    static void arrival(long due) { dueTick = due; }

    public static void frame(ExplosionShakeState.Angles angles) {
        if (OUTPUT.isEmpty() || !started || finished) return;
        if (!angles.equals(ExplosionShakeState.Angles.ZERO)) movingFrames++;
        TRACE.append(String.format(Locale.ROOT, "%s,%d,%d,%.7f,%.7f,%.7f,%d,%d%n",
                SCENES[scene], ticks, notifications, angles.yaw(), angles.pitch(), angles.roll(), ExplosionShakeHandler.ticks(), dueTick));
    }

    public static void tick() {
        if (OUTPUT.isEmpty() || finished) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof PauseScreen) mc.setScreen(null);
        if (mc.screen != null && mc.screen.getClass().getName().startsWith("ttv.migami.jeg.client.MagazineModeClient")) mc.screen.onClose();
        if (mc.screen != null) return;
        if (!prepared) {
            prepared = true;
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                var commands = server.getCommands();
                var source = server.createCommandSourceStack();
                commands.performPrefixedCommand(source, "difficulty peaceful");
                commands.performPrefixedCommand(source, "time set day");
                commands.performPrefixedCommand(source, "weather clear");
                commands.performPrefixedCommand(source, "gamemode creative @a");
                commands.performPrefixedCommand(source, "fill -32 -61 -32 32 -61 48 minecraft:stone");
                commands.performPrefixedCommand(source, "fill -16 -60 -16 16 -50 48 minecraft:air");
                var player = server.getPlayerList().getPlayer(mc.player.getUUID());
                if (player != null) { player.stopRiding(); player.connection.teleport(0, -60, 0, 0, 0); }
            });
        }
        if (++warmup < 40) return;
        if (!started) {
            started = true;
            originalStrength = Config.explosionScreenShake();
            originalCamera = mc.options.getCameraType();
        }
        mc.player.setYRot(0); mc.player.yRotO = 0;
        mc.player.setXRot(0); mc.player.xRotO = 0;
        if (ticks == 0) {
            notifications = movingFrames = 0;
            dueTick = -1;
            ExplosionShakeHandler.reset();
            Config.EXPLOSION_SCREEN_SHAKE.set(SCENES[scene].equals("disabled") ? 0 : 100);
            mc.options.setCameraType(SCENES[scene].equals("third-person") ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
            System.out.println("Explosion shake scene: " + SCENES[scene]);
            String name = SCENES[scene];
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                if (player == null) throw new IllegalStateException("Shake fixture player missing");
                var server = mc.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "fill -32 -61 -32 32 -61 48 minecraft:stone");
                if (!name.equals("mounted")) {
                    player.stopRiding();
                    if (mount != null) { mount.discard(); mount = null; }
                    player.connection.teleport(0, -60, 0, 0, 0);
                }
                trigger((ServerLevel) player.level(), player, name);
            });
        }
        mc.gui.getChat().clearMessages(false);
        if ((ticks == 18 || ticks == 25) && scene < 12) capture(mc, SCENES[scene] + "-" + ticks);
        ticks++;
        if (ticks < (SCENES[scene].equals("recovery") ? 260 : 65)) return;
        boolean excluded = SCENES[scene].equals("excluded");
        boolean disabled = SCENES[scene].equals("disabled");
        int expected = SCENES[scene].equals("c4-chain") || SCENES[scene].equals("continuous") ? 2 : 1;
        // Disabled clients still receive the server notification, then discard its visual state.
        if (notifications != (excluded ? 0 : expected) || (!excluded && !disabled && movingFrames == 0)
                || ((excluded || disabled) && movingFrames != 0)) {
            throw new IllegalStateException("Shake scene failed: " + SCENES[scene] + " notifications=" + notifications + " frames=" + movingFrames);
        }
        if (SCENES[scene].equals("recovery") && !ExplosionShakeHandler.frame().equals(ExplosionShakeState.Angles.ZERO)) {
            throw new IllegalStateException("Shake did not return to zero");
        }
        System.out.println("Explosion shake PASS: " + SCENES[scene] + " notifications=" + notifications + " movingFrames=" + movingFrames);
        ticks = 0;
        if (++scene < SCENES.length) return;
        finished = true;
        Config.EXPLOSION_SCREEN_SHAKE.set(originalStrength);
        mc.options.setCameraType(originalCamera);
        ExplosionShakeHandler.reset();
        try {
            Path output = Path.of(OUTPUT);
            Files.createDirectories(output);
            Files.writeString(output.resolve("shake.csv"), TRACE);
            Files.writeString(output.resolve("passed.txt"), "13 rendered explosion scenes passed\n");
        } catch (IOException e) { throw new IllegalStateException(e); }
        System.out.println("JEG explosion shake capture completed: " + OUTPUT);
        mc.stop();
    }

    private static void trigger(ServerLevel level, ServerPlayer player, String name) {
        Vec3 position = new Vec3(0, -59.5, 4);
        switch (name) {
            case "grenade" -> {
                GrenadeEntity grenade = new GrenadeEntity(level, player, 3, 60, false);
                grenade.setPos(position); level.addFreshEntity(grenade); grenade.explodeNow();
            }
            case "stun" -> {
                StunGrenadeEntity grenade = new StunGrenadeEntity(level, player, 60);
                grenade.setPos(position); level.addFreshEntity(grenade); grenade.explodeNow();
            }
            case "vehicle" -> {
                var vehicle = new ConfiguredVehicleEntity(ModEntities.TRUCK.get(), level, Reference.id("truck"));
                vehicle.setPos(position); level.addFreshEntity(vehicle);
                vehicle.hurt(level.damageSources().genericKill(), 100000);
            }
            case "missile" -> level.addFreshEntity(new VehicleMissileEntity(level, player, null,
                    position, new Vec3(0, -0.8, 0), Reference.id("javelin")));
            case "c4-chain" -> {
                var first = new PlacedExplosiveEntity(level, SpecialExplosiveItem.Kind.C4, player, position, 0);
                var second = new PlacedExplosiveEntity(level, SpecialExplosiveItem.Kind.C4, player, position.add(0, 0, 1), 0);
                level.addFreshEntity(first); level.addFreshEntity(second); first.detonate();
            }
            case "mounted" -> {
                mount = new ConfiguredVehicleEntity(ModEntities.TRUCK.get(), level, Reference.id("truck"));
                mount.setPos(0, -60, 0); level.addFreshEntity(mount); player.startRiding(mount, true);
                level.explode(null, position.x, position.y, position.z, 4, Level.ExplosionInteraction.NONE);
            }
            case "far" -> level.explode(null, 0, -60, 52, 16, Level.ExplosionInteraction.NONE);
            case "continuous" -> {
                level.explode(null, position.x, position.y, position.z, 4, Level.ExplosionInteraction.NONE);
                level.explode(null, position.x + 1, position.y, position.z, 4, Level.ExplosionInteraction.NONE);
            }
            case "excluded" -> {
                TimedThrowableItemProjectile[] projectiles = {
                        new MolotovCocktailEntity(level, player, 60), new WaterBombEntity(level, player, 60),
                        new SmokeGrenadeEntity(level, player, 40)};
                for (var projectile : projectiles) { projectile.setPos(position); level.addFreshEntity(projectile); projectile.explodeNow(); }
            }
            default -> {
                PrimedTnt tnt = new PrimedTnt(level, position.x, position.y, position.z, player);
                tnt.setFuse(1); level.addFreshEntity(tnt);
            }
        }
    }

    private static void capture(Minecraft mc, String name) {
        Path output = Path.of(OUTPUT);
        try { Files.createDirectories(output); } catch (IOException e) { throw new IllegalStateException(e); }
        Screenshot.grab(output.toFile(), name + ".png", mc.getMainRenderTarget(), message -> {});
    }
}
