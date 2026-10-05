package ttv.migami.jeg.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.advancement.FireWeaponAttribution;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFireDamageMixin {
    @Unique private float jeg$fireHealthBefore;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void jeg$beforeFireDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        this.jeg$fireHealthBefore = ((LivingEntity) (Object) this).getHealth();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void jeg$afterFireDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        FireWeaponAttribution.onDamage((LivingEntity) (Object) this, source, cir.getReturnValue(), this.jeg$fireHealthBefore);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void jeg$creditLethalFire(DamageSource source, CallbackInfo ci) {
        FireWeaponAttribution.onDeath((LivingEntity) (Object) this, source);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jeg$expireFireCredit(CallbackInfo ci) {
        FireWeaponAttribution.prune((LivingEntity) (Object) this);
    }
}
