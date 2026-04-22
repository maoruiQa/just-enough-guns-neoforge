package ttv.migami.jeg.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.item.GunItem;

@Mixin(AvatarRenderer.class)
public final class AvatarRendererMixin {
    @Inject(
            method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/entity/HumanoidArm;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void jeg$getArmPose(Avatar avatar, HumanoidArm arm, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        ItemStack stack = avatar.getMainArm() == arm ? avatar.getMainHandItem() : avatar.getOffhandItem();
        if (stack.isEmpty()) {
            return;
        }

        HumanoidModel.ArmPose extensionPose = IClientItemExtensions.of(stack).getArmPose(
                avatar instanceof AbstractClientPlayer player ? player : null,
                avatar.getMainArm() == arm ? net.minecraft.world.InteractionHand.MAIN_HAND : net.minecraft.world.InteractionHand.OFF_HAND,
                stack
        );
        if (extensionPose != HumanoidModel.ArmPose.EMPTY) {
            cir.setReturnValue(extensionPose);
            return;
        }

        if (stack.getItem() instanceof GunItem) {
            // Match NeoForge: guns use the crossbow-hold third-person pose.
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}

