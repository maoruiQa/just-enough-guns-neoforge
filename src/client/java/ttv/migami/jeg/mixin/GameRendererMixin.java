package ttv.migami.jeg.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.vehicle.client.VehicleCameraHandler;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {
    private static final double ADS_FOV_FACTOR = 0.35D;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void jeg$getFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gun)) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress(partialTick);
        if (ads <= 0.0F) {
            return;
        }

        double current = cir.getReturnValue();
        if (Reference.id("bolt_action_rifle").equals(gun.getStats().id())
                && GunScopeSupport.isBoltActionRifleScopeEnabled()) {
            cir.setReturnValue(Math.max(0.1D, Mth.lerp(ads, current, 20.0D)));
            return;
        }

        double factor = 1.0D - ADS_FOV_FACTOR * ads;
        cir.setReturnValue(Math.max(0.1D, current * factor));
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void jeg$getVehicleFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(VehicleCameraHandler.adjustFov(cir.getReturnValue()));
    }
}
