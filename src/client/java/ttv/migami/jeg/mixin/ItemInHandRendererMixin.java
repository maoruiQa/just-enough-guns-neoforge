package ttv.migami.jeg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.GunHandTransform;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.client.extensions.common.IClientItemExtensions;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.vehicle.client.VehicleCameraHandler;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public final class ItemInHandRendererMixin {
    @Unique
    private float jeg$capturedEquipProcess = Float.NaN;

    @Unique
    private float jeg$capturedSwingProcess = Float.NaN;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void jeg$captureArmRenderContext(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (VehicleCameraHandler.shouldHideHand()) {
            ci.cancel();
            return;
        }
        this.jeg$capturedEquipProcess = equipProgress;
        this.jeg$capturedSwingProcess = swingProgress;
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void jeg$clearArmRenderContext(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        this.jeg$capturedEquipProcess = Float.NaN;
        this.jeg$capturedSwingProcess = Float.NaN;
    }

    @Inject(
            method = "submitArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
            )
    )
    private void jeg$applyGunHandTransform(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState handsState,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        String gunPath = gun.getStats().id().getPath();
        if ("javelin".equals(gunPath) || "igla_9k38".equals(gunPath)) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float equipProcess = Float.isNaN(this.jeg$capturedEquipProcess) ? equipProgress : this.jeg$capturedEquipProcess;
        float swingProcess = Float.isNaN(this.jeg$capturedSwingProcess) ? swingProgress : this.jeg$capturedSwingProcess;
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        if (!extensions.applyForgeHandTransform(
                poseStack,
                player,
                arm,
                stack,
                partialTick,
                equipProcess,
                swingProcess
        )) {
            GunHandTransform.apply(poseStack, player, arm, gun.getStats(), partialTick);
        }
    }
}
