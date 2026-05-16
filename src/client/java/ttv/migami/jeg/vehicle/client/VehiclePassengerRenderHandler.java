package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehiclePassengerRenderHandler {
    private VehiclePassengerRenderHandler() {}

    public static boolean shouldRenderPlayer(AbstractClientPlayer renderedPlayer) {
        return !(renderedPlayer.getVehicle() instanceof VehicleEntity vehicle)
                || (!vehicle.shouldHidePassenger(renderedPlayer) && !shouldHideLocalZoomingHelicopterPassenger(renderedPlayer, vehicle));
    }

    private static boolean shouldHideLocalZoomingHelicopterPassenger(AbstractClientPlayer renderedPlayer, VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && renderedPlayer == player
                && vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }
}
