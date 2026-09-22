package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ttv.migami.jeg.util.GunSwingReset;
import ttv.migami.jeg.util.GunSwingStateReset;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGunSwingMixin implements GunSwingReset {
    @Shadow
    private LivingEntity.SwingState swingState;

    @Override
    public void jeg$resetSwing() {
        ((GunSwingStateReset) (Object) this.swingState).jeg$reset();
    }
}
