package ttv.migami.jeg.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.item.GunItem;

@Mixin(ArmedEntityRenderState.class)
public final class ArmedEntityRenderStateMixin {
    @Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
    private static void jeg$extractArmedEntityRenderState(
            LivingEntity entity,
            ArmedEntityRenderState state,
            ItemModelResolver resolver,
            float partialTick,
            CallbackInfo ci
    ) {
        if (state.rightHandItemStack != null && state.rightHandItemStack.getItem() instanceof GunItem) {
            state.rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        if (state.leftHandItemStack != null && state.leftHandItemStack.getItem() instanceof GunItem) {
            state.leftArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
    }
}
