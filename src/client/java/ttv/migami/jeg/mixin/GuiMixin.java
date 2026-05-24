package ttv.migami.jeg.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.ClientHudRenderer;
import ttv.migami.jeg.client.CrosshairHandler;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.client.HappyGhastArmorHud;
import ttv.migami.jeg.client.ScopeOverlayRenderer;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.vehicle.client.overlay.VehicleHudOverlay;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(Gui.class)
public final class GuiMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void jeg$hideCrosshairWhenHoldingGun(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (VehicleHudOverlay.isVehicleHudActive()) {
            ci.cancel();
            return;
        }
        if (minecraft.player != null && (minecraft.player.getMainHandItem().getItem() instanceof GunItem || minecraft.player.getOffhandItem().getItem() instanceof GunItem)) {
            ScopeOverlayRenderer.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            CrosshairHandler.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void jeg$hideVehicleHotbar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VehicleHudOverlay.shouldReplaceHotbar()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jeg$renderOverheatBar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            if (VehicleHudOverlay.isVehicleHudActive()) {
                VehicleHudOverlay.renderVehicleHud(guiGraphics);
            }
            FabricClientBootstrap.renderThrowableEffectOverlay(guiGraphics);
            FabricClientBootstrap.renderOverheatBar(guiGraphics);
            ClientHudRenderer.render(guiGraphics);
            HappyGhastArmorHud.render(guiGraphics);
        }
    }

    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void jeg$hidePlayerHealthInVehicle(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        if (jeg$isVehicleHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void jeg$hidePlayerFoodInVehicle(GuiGraphicsExtractor guiGraphics, Player player, int y, int rightX, CallbackInfo ci) {
        if (jeg$isVehicleHudActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "nextContextualInfoState", at = @At("HEAD"), cancellable = true)
    private void jeg$hideExperienceInfoInVehicle(CallbackInfoReturnable<Object> ci) {
        if (jeg$isVehicleHudActive()) {
            ci.setReturnValue(jeg$emptyContextualInfo());
        }
    }

    private static boolean jeg$isVehicleHudActive() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getVehicle() instanceof VehicleEntity;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object jeg$emptyContextualInfo() {
        try {
            Class<? extends Enum> enumClass = Class.forName("net.minecraft.client.gui.Gui$ContextualInfo").asSubclass(Enum.class);
            return Enum.valueOf(enumClass, "EMPTY");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing Gui.ContextualInfo", e);
        }
    }
}
