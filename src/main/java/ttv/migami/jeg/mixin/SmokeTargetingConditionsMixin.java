package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.util.SmokeUtil;

@Mixin(TargetingConditions.class)
public abstract class SmokeTargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void jeg$rejectSmoke(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (jeg$isBlindedBySmoke(attacker) || jeg$isBlindedBySmoke(target)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean jeg$isBlindedBySmoke(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.hasEffect(ModEffects.SMOKED) || SmokeUtil.isInSmoke(entity);
    }
}
