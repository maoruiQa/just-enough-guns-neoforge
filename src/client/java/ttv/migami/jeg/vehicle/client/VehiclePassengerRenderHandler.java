package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehiclePassengerRenderHandler {
    private VehiclePassengerRenderHandler() {}

    public static boolean shouldHideLocalPlayerPassenger(int renderStateId) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && renderStateId == player.getId()
                && player.getVehicle() instanceof VehicleEntity vehicle
                && shouldHideLocalPlayerPassenger(vehicle);
    }

    public static boolean shouldHideLocalPlayerPassenger(VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && player.getVehicle() == vehicle
                && (vehicle.shouldHidePassenger(player) || shouldHideLocalZoomingHelicopterPassenger(vehicle));
    }

    private static boolean shouldHideLocalZoomingHelicopterPassenger(VehicleEntity vehicle) {
        return vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }
}
