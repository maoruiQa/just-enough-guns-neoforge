package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.sensing.Sensing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Redirect(method = "canContinueToUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/sensing/Sensing;hasLineOfSight(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean jeg$hasLineOfSight(Sensing sensing, net.minecraft.world.entity.Entity target) {
        if (this.mob.entityTags().contains("MobGunner") && target instanceof LivingEntity) {
            return true;
        }
        return sensing.hasLineOfSight(target);
    }
}
