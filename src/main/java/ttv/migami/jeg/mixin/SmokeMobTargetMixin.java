package ttv.migami.jeg.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.util.SmokeUtil;

@Mixin(Mob.class)
public abstract class SmokeMobTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity jeg$rejectSmokeTargets(LivingEntity target) {
        Mob mob = (Mob) (Object) this;
        if (jeg$isBlindedBySmoke(mob)) {
            return null;
        }
        if (target != null && jeg$isBlindedBySmoke(target)) {
            return null;
        }
        return target;
    }

    @Inject(method = "serverAiStep", at = {@At("HEAD"), @At("TAIL")})
    private void jeg$clearSmokeTargets(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (jeg$isBlindedBySmoke(mob) || jeg$isBlindedBySmoke(mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                    .filter(SmokeMobTargetMixin::jeg$isBlindedBySmoke)
                    .ifPresent(target -> mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET));
            if (jeg$isBlindedBySmoke(mob)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
    }

    private static boolean jeg$isBlindedBySmoke(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        return entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SMOKED.get()))
                || SmokeUtil.isInSmoke(entity);
    }
}
