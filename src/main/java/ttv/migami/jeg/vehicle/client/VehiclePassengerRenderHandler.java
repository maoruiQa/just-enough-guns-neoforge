package ttv.migami.jeg.vehicle.client;

import com.google.common.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleController;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class VehiclePassengerRenderHandler {
    private static final ContextKey<LivingEntity> RENDER_ENTITY =
            new ContextKey<>(Identifier.fromNamespaceAndPath(Reference.MOD_ID, "render_entity"));

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

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntity entity = event.getRenderState().getRenderData(RENDER_ENTITY);
        if (entity == null || entity.entityTags().contains(EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG)) {
            return;
        }
        if (entity.getVehicle() instanceof VehicleEntity vehicle && vehicle.shouldHidePassenger(entity)) {
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

    @SubscribeEvent
    public static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {}, (entity, state) -> {
            state.setRenderData(RENDER_ENTITY, entity);
        });
    }
}
