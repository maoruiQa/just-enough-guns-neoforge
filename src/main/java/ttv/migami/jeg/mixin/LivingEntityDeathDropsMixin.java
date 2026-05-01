package ttv.migami.jeg.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.event.GunEvents;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathDropsMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void jeg$spawnGunnerDrops(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        GunEvents.spawnGunnerDrops((LivingEntity) (Object) this);
    }
}
