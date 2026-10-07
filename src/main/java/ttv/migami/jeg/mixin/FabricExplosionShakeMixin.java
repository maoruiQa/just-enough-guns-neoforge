package ttv.migami.jeg.mixin;

import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.network.NetworkHandler;

/** Fabric server hook; NeoForge uses ExplosionEvent.Detonate instead. */
@Mixin(ServerExplosion.class)
public abstract class FabricExplosionShakeMixin {
    @Unique private boolean jeg$shakeSent;

    @Inject(method = "explode", at = @At("TAIL"))
    private void jeg$notifyShake(CallbackInfoReturnable<Integer> callback) {
        ServerExplosion explosion = (ServerExplosion) (Object) this;
        if (!jeg$shakeSent) {
            jeg$shakeSent = true;
            NetworkHandler.sendExplosionShake(explosion.level(), explosion.center(), explosion.radius());
        }
    }
}
