package ttv.migami.jeg.vehicle.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.network.VehicleChangeSeatPayload;
import ttv.migami.jeg.vehicle.network.VehicleDismountPayload;
import ttv.migami.jeg.vehicle.network.VehicleInputPayload;
import ttv.migami.jeg.vehicle.network.VehicleOpenMenuPayload;

/** Opt-in localhost multiplayer regression probe, using the ordinary server-validated payloads. */
public final class VehicleNetworkCheck {
    private static final String OUTPUT = System.getProperty("jeg.vehicleNetworkCheck", "");
    private static final String ROLE = System.getProperty("jeg.vehicleNetworkRole", "observer");
    private static int vehicleId = -1, ticks;

    private VehicleNetworkCheck() {}

    public static boolean tick(Minecraft mc) {
        if (OUTPUT.isEmpty()) return false;
        if (Files.exists(Path.of(OUTPUT).resolve("stop"))) { mc.stop(); return true; }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return false;
        var candidates = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(256),
                entity -> entity instanceof VehicleEntity vehicle && vehicle.usesSwControls());
        if (candidates.isEmpty()) return true;
        var vehicle = (VehicleEntity) candidates.getLast();
        if (vehicleId != vehicle.getId()) { vehicleId = vehicle.getId(); ticks = 0; VehicleClientState.clear(); }
        mc.gui.hud.getChat().clearMessages(false);
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        boolean riding = mc.player.getVehicle() == vehicle;
        boolean driver = ROLE.equals("driver");
        boolean observer = ROLE.equals("observer");
        boolean aiming = !driver && riding && vehicle.canPassengerUseSelectedVehicleWeapon(mc.player);
        if (riding) VehicleClientState.update(vehicle, false, aiming, false);
        if (!driver) { mc.player.setYRot(45); mc.player.setXRot(-20); }
        boolean controls = mc.gui.screen() == null && ticks < 90;
        boolean forward = controls && (driver ? ticks < 60 : true);
        boolean reverse = controls && driver && ticks >= 60;
        var payload = new VehicleInputPayload(vehicleId, forward, reverse, false, false, false,
                controls && driver && ticks < 60, false, false, false, false, false, false,
                -1, false, false, observer ? 2000 : 0, observer ? 2000 : 0, aiming);
        if (riding) vehicle.processClientInput(mc.player, payload.toInput());
        // The observer deliberately tries to control a vehicle without occupying it.
        mc.getConnection().send(payload);
        if (ticks == 95) mc.getConnection().send(new VehicleOpenMenuPayload(vehicleId));
        if (ticks == 110 && mc.gui.screen() != null) { mc.player.closeContainer(); mc.gui.setScreen(null); }
        if (ticks == 120 && driver && riding) mc.getConnection().send(new VehicleChangeSeatPayload(vehicleId));
        if (ticks == 140 && driver && riding) mc.getConnection().send(new VehicleDismountPayload(vehicleId));
        try {
            Path output = Path.of(OUTPUT);
            Files.createDirectories(output);
            var velocity = vehicle.getDeltaMovement();
            String row = vehicle.vehicleDataId().getPath() + "," + vehicleId + "," + ticks + "," + mc.level.getGameTime()
                    + "," + (riding ? vehicle.getSeatIndex(mc.player) : -1) + "," + vehicle.getX() + "," + vehicle.getY() + "," + vehicle.getZ()
                    + "," + velocity.x + "," + velocity.y + "," + velocity.z + "," + vehicle.getYRot()
                    + "," + vehicle.enginePower() + "," + vehicle.vehicleEnergy() + "," + vehicle.vehicleHealth()
                    + "," + vehicle.partHealthFraction(OBBInfo.Part.MAIN_ENGINE) + "," + mc.player.containerMenu.slots.size()
                    + "," + (mc.gui.screen() != null) + "," + vehicle.isPassengerAiming(mc.player)
                    + "," + vehicle.engineStarted() + "," + vehicle.engineReady() + "\n";
            Files.writeString(output.resolve("network.csv"), row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (ticks == 105 || ticks == 145) Screenshot.grab(output.toFile(), vehicle.vehicleDataId().getPath()
                    + "-" + ticks + ".png", mc.gameRenderer.mainRenderTarget(), 1, message -> {});
        } catch (IOException error) { throw new IllegalStateException("Cannot record multiplayer regression", error); }
        ticks++;
        return true;
    }
}
