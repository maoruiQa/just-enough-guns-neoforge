package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.CameraPos;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehicleCameraHandler {
    private static final double VEHICLE_ZOOM_DIVISOR = 3.0D;

    private VehicleCameraHandler() {}

    @SubscribeEvent
    public static void onCalculateDetachedCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!VehicleClientState.isRidingVehicle() || VehicleClientState.vehicleId() != vehicle.getId()) {
            return;
        }
        double configuredDistance = Math.abs(vehicle.vehicleData().defaults().thirdPersonCamera().z());
        if (configuredDistance > 0.0D) {
            event.setDistance((float) configuredDistance);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!VehicleClientState.isRidingVehicle() || VehicleClientState.vehicleId() != vehicle.getId()) {
            return;
        }
        CameraPos camera = vehicle.vehicleData().defaults().thirdPersonCamera();
        if (camera.x() != 0.0D || camera.y() != 0.0D) {
            event.getCamera().move(0.0F, (float) camera.y(), (float) -camera.x());
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (!VehicleClientState.isRidingVehicle() || VehicleClientState.vehicleId() != vehicle.getId() || !VehicleClientState.zoomDown()) {
            return;
        }
        event.setFOV(event.getFOV() / VEHICLE_ZOOM_DIVISOR);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        if (VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.freeLookDown()
                && vehicle.vehicleData().defaults().allowFreeCam()) {
            event.setCanceled(true);
        }
    }
}
