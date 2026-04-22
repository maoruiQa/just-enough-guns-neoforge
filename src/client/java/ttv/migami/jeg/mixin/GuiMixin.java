package ttv.migami.jeg.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.FabricClientBootstrap;
import ttv.migami.jeg.item.GunItem;

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
        if (player.getMainHandItem().getItem() instanceof GunItem || player.getOffhandItem().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }
}
