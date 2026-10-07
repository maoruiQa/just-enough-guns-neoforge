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
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float jeg$enemyVehicleDamage(float amount, ServerLevel level, DamageSource source, float original) {
        return EnemyVehicleCombat.limitDamage((LivingEntity) (Object) this, source, amount);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dealDefaultKnockback(Lnet/minecraft/world/damagesource/DamageSource;FZ)V"))
    private void jeg$enemyVehicleHurtImpulse(LivingEntity target, DamageSource source, float damage, boolean blocked) {
        Vec3 before = target.getDeltaMovement();
        target.dealDefaultKnockback(source, damage, blocked);
        target.setDeltaMovement(before.add(EnemyVehicleCombat.limitImpulse(target, source, target.getDeltaMovement().subtract(before))));
    }
}
