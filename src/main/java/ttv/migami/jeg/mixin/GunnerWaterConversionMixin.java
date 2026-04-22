package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class GunnerWaterConversionMixin {
    @Inject(method = "isUnderWaterConverting", at = @At("HEAD"), cancellable = true)
    private void jeg$preventGunnerWaterConversion(CallbackInfoReturnable<Boolean> cir) {
        Mob mob = (Mob) (Object) this;
        if (mob.entityTags().contains("MobGunner") || mob.entityTags().contains("jeg_pillager_gunner")) {
            cir.setReturnValue(false);
        }
    }
}
