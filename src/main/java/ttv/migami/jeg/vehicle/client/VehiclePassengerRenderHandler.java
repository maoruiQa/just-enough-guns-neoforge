package ttv.migami.jeg.vehicle.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehiclePassengerRenderHandler {
    private VehiclePassengerRenderHandler() {}

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null
                && event.getRenderState() instanceof AvatarRenderState renderState
                && renderState.id == player.getId()
                && player.getVehicle() instanceof VehicleEntity vehicle
                && (vehicle.shouldHidePassenger(player) || shouldHideLocalZoomingHelicopterPassenger(event, vehicle))) {
            event.setCanceled(true);
        }
    }

    private static boolean shouldHideLocalZoomingHelicopterPassenger(RenderPlayerEvent.Pre event, VehicleEntity vehicle) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && event.getRenderState() instanceof AvatarRenderState renderState
                && renderState.id == player.getId()
                && vehicle.vehicleData().defaults().vehicleType() == VehicleType.HELICOPTER
                && VehicleClientState.isRidingVehicle()
                && VehicleClientState.vehicleId() == vehicle.getId()
                && VehicleClientState.zoomDown();
    }
}
