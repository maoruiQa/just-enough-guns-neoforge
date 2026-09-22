package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import ttv.migami.jeg.client.SwingStateAccess;

@Mixin(LivingEntity.SwingState.class)
public class SwingStateMixin implements SwingStateAccess {
    @Shadow
    private LivingEntity.SwingDescription currentSwing;

    @Shadow
    private int ticks;

    @Shadow
    private float oldAnimation;

    @Shadow
    private float animation;

    @Override
    public void jeg$clear() {
        this.currentSwing = null;
        this.ticks = 0;
        this.oldAnimation = 0.0F;
        this.animation = 0.0F;
    }
}
