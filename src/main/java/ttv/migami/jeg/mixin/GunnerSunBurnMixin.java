package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class GunnerSunBurnMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void jeg$preventGunnerSunBurn(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob) (Object) this;
        if (mob.getTags().contains("MobGunner") || mob.getTags().contains("jeg_pillager_gunner")) {
            cir.setReturnValue(false);
        }
    }
}
