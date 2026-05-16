package ttv.migami.jeg.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.ClientHudRenderer;
import ttv.migami.jeg.client.CrosshairHandler;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.ScopeOverlayRenderer;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.vehicle.client.overlay.VehicleHudOverlay;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(Gui.class)
public final class GuiMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void jeg$renderOverheatBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        FabricClientBootstrap.renderThrowableEffectOverlay(guiGraphics);
        FabricClientBootstrap.renderOverheatBar(guiGraphics);
        VehicleHudOverlay.renderHud(guiGraphics);
        ClientHudRenderer.render(guiGraphics);
        if (player.getVehicle() instanceof VehicleEntity
                || player.getMainHandItem().getItem() instanceof GunItem
                || player.getOffhandItem().getItem() instanceof GunItem) {
            ScopeOverlayRenderer.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            CrosshairHandler.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void jeg$hideVehicleHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null
                && player.getVehicle() instanceof VehicleEntity vehicle
                && VehicleHudOverlay.shouldReplaceHotbar(player, vehicle)) {
            ci.cancel();
        }
    }
}
