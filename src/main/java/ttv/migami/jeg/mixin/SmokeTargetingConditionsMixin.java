package ttv.migami.jeg.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.init.ModEffects;

@Mixin(TargetingConditions.class)
public abstract class SmokeTargetingConditionsMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void jeg$rejectSmokedPlayer(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof Player player && player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SMOKED.get()))) {
            cir.setReturnValue(false);
        }
    }
}
