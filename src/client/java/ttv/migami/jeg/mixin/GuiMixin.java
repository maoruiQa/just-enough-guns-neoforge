package ttv.migami.jeg.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.ClientHudRenderer;
import ttv.migami.jeg.client.CrosshairHandler;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.item.GunItem;

@Mixin(Gui.class)
public final class GuiMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void jeg$hideCrosshairWhenHoldingGun(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && (minecraft.player.getMainHandItem().getItem() instanceof GunItem || minecraft.player.getOffhandItem().getItem() instanceof GunItem)) {
            CrosshairHandler.render(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void jeg$renderOverheatBar(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null) {
            FabricClientBootstrap.renderThrowableEffectOverlay(guiGraphics);
            FabricClientBootstrap.renderOverheatBar(guiGraphics);
            ClientHudRenderer.render(guiGraphics);
        }
    }
}
