package ttv.migami.jeg.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.item.GunItem;

@Mixin(AbstractClientPlayer.class)
public final class AbstractClientPlayerMixin {
    private static final float ADS_FOV_FACTOR = 0.35F;

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void jeg$applyAdsFovModifier(boolean firstPerson, float tickProgress, CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof GunItem)) {
            return;
        }

        float ads = AimingHandler.get().getNormalisedAdsProgress(tickProgress);
        if (ads <= 0.0F) {
            return;
        }

        float factor = 1.0F - ADS_FOV_FACTOR * ads;
        cir.setReturnValue(Math.max(0.1F, cir.getReturnValueF() * factor));
    }
}
