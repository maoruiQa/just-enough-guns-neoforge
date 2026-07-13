package ttv.migami.jeg.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ttv.migami.jeg.init.ModEffects;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Redirect(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/Sensing;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jeg$hasLineOfSight(Sensing sensing, net.minecraft.world.entity.Entity target) {
        if (target instanceof Player player && player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.SMOKED.get()))) {
            return false;
        }
        if (this.mob.getTags().contains("MobGunner") && target instanceof Player player && player.isInvisible()) {
            return false;
        }
        if (this.mob.getTags().contains("MobGunner") && target instanceof LivingEntity) {
            return true;
        }
        return sensing.hasLineOfSight(target);
    }
}
