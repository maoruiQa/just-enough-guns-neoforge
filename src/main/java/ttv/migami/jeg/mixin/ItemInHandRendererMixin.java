package ttv.migami.jeg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.GunHandTransform;
import ttv.migami.jeg.item.GunItem;

@Mixin(ItemInHandRenderer.class)
public final class ItemInHandRendererMixin {
    @Unique
    private float jeg$capturedEquipProcess = Float.NaN;

    @Unique
    private float jeg$capturedSwingProcess = Float.NaN;

    @Unique
    private ItemStack jeg$currentFirstPersonStack = ItemStack.EMPTY;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void jeg$captureArmRenderContext(
            AbstractClientPlayer player,
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
        this.jeg$capturedEquipProcess = equipProgress;
        this.jeg$capturedSwingProcess = swingProgress;
        this.jeg$currentFirstPersonStack = stack;
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void jeg$clearArmRenderContext(
            AbstractClientPlayer player,
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
        this.jeg$currentFirstPersonStack = ItemStack.EMPTY;
    }

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void jeg$suppressHeavyGunLeftArm(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            float equipProgress,
            float swingProgress,
            HumanoidArm arm,
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

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void jeg$renderItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(entity instanceof LocalPlayer player)) {
            return;
        }
        if (!(stack.getItem() instanceof GunItem gun)) {
            return;
        }

        HumanoidArm arm;
        if (displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            arm = HumanoidArm.RIGHT;
        } else if (displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            arm = HumanoidArm.LEFT;
        } else {
            return;
        }

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float equipProcess = Float.isNaN(this.jeg$capturedEquipProcess) ? 0.0F : this.jeg$capturedEquipProcess;
        float swingProcess = Float.isNaN(this.jeg$capturedSwingProcess) ? 0.0F : this.jeg$capturedSwingProcess;
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
