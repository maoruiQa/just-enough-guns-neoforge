package ttv.migami.jeg.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ttv.migami.jeg.init.ModDamageTypes;
import ttv.migami.jeg.vehicle.ai.EnemyVehicleCombat;

/** Retain native block drops/hooks while matching SW's weak block blast ray power. */
@Mixin(ServerExplosion.class)
public abstract class VehicleExplosionMixin {
    @Shadow @Final private DamageSource damageSource;
    @Unique private final Map<Entity, Vec3> jeg$impulses = new IdentityHashMap<>();

    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"))
    private void jeg$limitImpulse(Entity entity, Vec3 impulse) {
        Vec3 limited = EnemyVehicleCombat.limitImpulse(entity, this.damageSource, impulse);
        this.jeg$impulses.put(entity, limited);
        entity.push(limited);
    }

    @Redirect(method = "hurtEntities", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object jeg$syncLimitedImpulse(Map<Object, Object> players, Object player, Object impulse) {
        return players.put(player, this.jeg$impulses.getOrDefault(player, (Vec3) impulse));
    }

    @ModifyConstant(method = "calculateExplodedPositions", constant = @Constant(floatValue = .7F))
    private float jeg$vehicleBlastBase(float original) {
        return this.damageSource.is(ModDamageTypes.CUSTOM_EXPLOSION) ? .2F : original;
    }

    @ModifyConstant(method = "calculateExplodedPositions", constant = @Constant(floatValue = .6F))
    private float jeg$vehicleBlastVariation(float original) {
        return this.damageSource.is(ModDamageTypes.CUSTOM_EXPLOSION) ? .15F : original;
    }
}
