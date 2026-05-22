package ttv.migami.jeg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.item.HappyGhastArmorHelper;

@Mixin(LivingEntity.class)
public final class HappyGhastArmorDamageMixin {
    private static final ThreadLocal<Boolean> JEG_BYPASS_ARMOR = ThreadLocal.withInitial(() -> false);

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void jeg$applyArmoredHarnessDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (JEG_BYPASS_ARMOR.get()) {
            return;
        }

        if (!((Object) this instanceof HappyGhast ghast)) {
            return;
        }

        HappyGhastArmorHelper.ArmorDamageResult result = HappyGhastArmorHelper.applyIncomingDamage(ghast, source, amount);
        if (!result.armorHit()) {
            return;
        }
        if (result.finalDamage() <= 0.0F) {
            cir.setReturnValue(false);
            return;
        }

        JEG_BYPASS_ARMOR.set(true);
        try {
            cir.setReturnValue(ghast.hurtServer(level, source, result.finalDamage()));
        } finally {
            JEG_BYPASS_ARMOR.set(false);
        }
    }
}
