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
import ttv.migami.jeg.client.GunItemClientExtensions;
import ttv.migami.jeg.item.GunItem;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public final class ItemInHandRendererMixin {
    @Unique
    private ItemStack jeg$currentFirstPersonStack = ItemStack.EMPTY;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"))
    private void jeg$captureArmRenderContext(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        this.jeg$currentFirstPersonStack = itemStack;
    }

    @Inject(method = "submitArmWithItem", at = @At("RETURN"))
    private void jeg$clearArmRenderContext(
            PlayerRenderState playerState,
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        this.jeg$currentFirstPersonStack = ItemStack.EMPTY;
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
            FirstPersonHandsAndItemsRenderState state,
            float partialTicks,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(itemStack.getItem() instanceof GunItem gun)) {
            return;
        }

        String gunPath = gun.getStats().id().getPath();
        if ("javelin".equals(gunPath) || "igla_9k38".equals(gunPath)) {
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
                ? player.getMainArm()
                : player.getMainArm().getOpposite();
        GunItemClientExtensions.applyForStats(
                gun.getStats(),
                poseStack,
                player,
                arm,
                partialTicks,
                inverseArmHeight,
                attack
        );
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void jeg$suppressHeavyGunLeftArm(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            float inverseArmHeight,
            float attackValue,
            HumanoidArm arm,
            PlayerRenderState playerState,
            CallbackInfo ci
    ) {
        if (arm != HumanoidArm.LEFT) {
            return;
        }
        if (!(this.jeg$currentFirstPersonStack.getItem() instanceof GunItem gun)) {
            return;
        }

        String gunId = gun.getStats().id().getPath();
        if ("rocket_launcher".equals(gunId) || "typhoonee".equals(gunId)) {
            ci.cancel();
        }
    }
}
