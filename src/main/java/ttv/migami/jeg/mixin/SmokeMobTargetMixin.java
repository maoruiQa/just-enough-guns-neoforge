package ttv.migami.jeg.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.init.ModEffects;

@Mixin(Mob.class)
public abstract class SmokeMobTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity jeg$rejectSmokedPlayerTarget(LivingEntity target) {
        return jeg$isSmokedPlayer(target) ? null : target;
    }

    @Inject(method = "serverAiStep", at = {@At("HEAD"), @At("TAIL")})
    private void jeg$clearSmokedPlayerTarget(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (jeg$isSmokedPlayer(mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(SmokeMobTargetMixin::jeg$isSmokedPlayer)
                .ifPresent(target -> mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET));
        }
    }

    private static boolean jeg$isSmokedPlayer(LivingEntity target) {
        return target instanceof Player player
            && player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SMOKED.get()));
    }
}
