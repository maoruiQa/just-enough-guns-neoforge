package ttv.migami.jeg.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import ttv.migami.jeg.init.ModDamageTypes;

/** Retain native block drops/hooks while matching SW's weak block blast ray power. */
@Mixin(ServerExplosion.class)
public abstract class VehicleExplosionMixin {
    @Shadow @Final private DamageSource damageSource;

    @ModifyConstant(method = "calculateExplodedPositions", constant = @Constant(floatValue = .7F))
    private float jeg$vehicleBlastBase(float original) {
        return this.damageSource.is(ModDamageTypes.CUSTOM_EXPLOSION) ? .2F : original;
    }

    @ModifyConstant(method = "calculateExplodedPositions", constant = @Constant(floatValue = .6F))
    private float jeg$vehicleBlastVariation(float original) {
        return this.damageSource.is(ModDamageTypes.CUSTOM_EXPLOSION) ? .15F : original;
    }
}
