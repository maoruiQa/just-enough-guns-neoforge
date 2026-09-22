package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ttv.migami.jeg.client.GunSwingReset;

@Mixin(LivingEntity.class)
public class LivingEntitySwingResetMixin implements GunSwingReset {
    @Shadow
    private LivingEntity.SwingState swingState;

    @Override
    public void jeg$suppressGunSwing() {
        ((SwingStateAccess) (Object) this.swingState).jeg$clear();
    }
}
