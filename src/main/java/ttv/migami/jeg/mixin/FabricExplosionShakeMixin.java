package ttv.migami.jeg.mixin;

import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.network.NetworkHandler;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

/** Fabric server hook; NeoForge uses ExplosionEvent.Detonate instead. */
@Mixin(Explosion.class)
public abstract class FabricExplosionShakeMixin {
    @Unique private boolean jeg$shakeSent;
    @Shadow @Final private Level level;

    @Inject(method = "explode", at = @At("TAIL"))
    private void jeg$notifyShake(CallbackInfo callback) {
        Explosion explosion = (Explosion) (Object) this;
        if (!jeg$shakeSent && level instanceof ServerLevel server) {
            jeg$shakeSent = true;
            NetworkHandler.sendExplosionShake(server, explosion.center(), explosion.radius());
        }
    }
}
