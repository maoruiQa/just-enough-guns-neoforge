package ttv.migami.jeg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat;

/** Clamp the actual server argument, including Fabric's boolean-only damage bridge. */
@Mixin(LivingEntity.class)
public abstract class EnemyVehicleDamageMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float jeg$enemyVehicleDamage(float amount, DamageSource source, float original) {
        return EnemyVehicleCombat.limitDamage((LivingEntity) (Object) this, source, amount);
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void jeg$enemyVehicleHurtImpulse(LivingEntity target, double power, double x, double z, DamageSource source, float damage) {
        Vec3 before = target.getDeltaMovement();
        target.knockback(power, x, z);
        target.setDeltaMovement(before.add(EnemyVehicleCombat.limitImpulse(target, source, target.getDeltaMovement().subtract(before))));
    }
}
