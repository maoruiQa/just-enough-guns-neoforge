package ttv.migami.jeg.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.client.GunHandTransform;
import ttv.migami.jeg.client.GunItemClientExtensions;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

@Mixin(ItemInHandRenderer.class)
public final class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Unique
    private LocalPlayer jeg$capturedPlayer;

    @Unique
    private ItemStack jeg$capturedStack = ItemStack.EMPTY;

    @Unique
    private float jeg$capturedPartialTick = Float.NaN;

    @Unique
    private float jeg$capturedEquipProcess = Float.NaN;

    @Unique
    private float jeg$capturedSwingProcess = Float.NaN;

    @Unique
    private boolean jeg$customTransformApplied;

    @Unique
    private ItemStack jeg$preTickMainHandItem = ItemStack.EMPTY;

    @Unique
    private ItemStack jeg$preTickOffHandItem = ItemStack.EMPTY;

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
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        this.jeg$capturedPlayer = player instanceof LocalPlayer local ? local : null;
        this.jeg$capturedStack = stack;
        this.jeg$capturedPartialTick = partialTick;
        this.jeg$capturedEquipProcess = equipProgress;
        this.jeg$capturedSwingProcess = swingProgress;
        this.jeg$customTransformApplied = false;
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
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        this.jeg$capturedPlayer = null;
        this.jeg$capturedStack = ItemStack.EMPTY;
        this.jeg$capturedPartialTick = Float.NaN;
        this.jeg$capturedEquipProcess = Float.NaN;
        this.jeg$capturedSwingProcess = Float.NaN;
        this.jeg$customTransformApplied = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void jeg$capturePreTickHandItems(CallbackInfo ci) {
        this.jeg$preTickMainHandItem = this.mainHandItem.copy();
        this.jeg$preTickOffHandItem = this.offHandItem.copy();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jeg$stabilizeVolatileGunSwaps(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        ItemStack liveMain = player.getMainHandItem();
        if (jeg$isVolatileGunComponentDiff(this.mainHandItem, liveMain)
                || jeg$isVolatileGunComponentDiff(this.jeg$preTickMainHandItem, liveMain)) {
            this.mainHandItem = liveMain;
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
        }

        ItemStack liveOff = player.getOffhandItem();
        if (jeg$isVolatileGunComponentDiff(this.offHandItem, liveOff)
                || jeg$isVolatileGunComponentDiff(this.jeg$preTickOffHandItem, liveOff)) {
            this.offHandItem = liveOff;
            this.offHandHeight = 1.0F;
            this.oOffHandHeight = 1.0F;
        }
    }

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
            )
    )
    private void jeg$wrapItemArmTransform(
            ItemInHandRenderer instance,
            PoseStack poseStack,
            HumanoidArm arm,
            float equipProgress,
            Operation<Void> original
    ) {
        if (jeg$tryApplyCustomTransform(poseStack, arm)) {
            return;
        }
        original.call(instance, poseStack, arm, equipProgress);
    }

    @WrapOperation(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmAttackTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
            )
    )
    private void jeg$wrapItemArmAttackTransform(
            ItemInHandRenderer instance,
            PoseStack poseStack,
            HumanoidArm arm,
            float swingProgress,
            Operation<Void> original
    ) {
        if (jeg$tryApplyCustomTransform(poseStack, arm)) {
            return;
        }
        original.call(instance, poseStack, arm, swingProgress);
    }

    @Unique
    private boolean jeg$tryApplyCustomTransform(PoseStack poseStack, HumanoidArm arm) {
        if (this.jeg$customTransformApplied) {
            return true;
        }
        LocalPlayer player = this.jeg$capturedPlayer;
        ItemStack stack = this.jeg$capturedStack;
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof GunItem gun)) {
            return false;
        }
        float partialTick = Float.isNaN(this.jeg$capturedPartialTick) ? 0.0F : this.jeg$capturedPartialTick;
        float equipProcess = Float.isNaN(this.jeg$capturedEquipProcess) ? 0.0F : this.jeg$capturedEquipProcess;
        float swingProcess = Float.isNaN(this.jeg$capturedSwingProcess) ? 0.0F : this.jeg$capturedSwingProcess;

        // Match NeoForge behavior: only apply hand transform if the item's extensions explicitly handle it.
        // For AnimatedGunItem, only holy_shotgun returns true from applyForgeHandTransform.
        // All other guns return false and rely solely on AnimatedGunRenderer's ADS transform.
        IClientItemExtensions extensions = IClientItemExtensions.of(stack);
        boolean applied = extensions.applyForgeHandTransform(
                poseStack,
                player,
                arm,
                stack,
                partialTick,
                equipProcess,
                swingProcess
        );

        this.jeg$customTransformApplied = applied;
        return applied;
    }

    @Unique
    private static boolean jeg$isVolatileGunComponentDiff(ItemStack visibleStack, ItemStack liveStack) {
        if (visibleStack == null || liveStack == null || visibleStack.isEmpty() || liveStack.isEmpty()) {
            return false;
        }
        if (!ItemStack.isSameItem(visibleStack, liveStack)) {
            return false;
        }
        if (!(liveStack.getItem() instanceof GunItem)) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(visibleStack, liveStack)) {
            return false;
        }

        ItemStack visibleStable = visibleStack.copy();
        ItemStack liveStable = liveStack.copy();
        jeg$stripVolatileGunRenderComponents(visibleStable);
        jeg$stripVolatileGunRenderComponents(liveStable);
        return ItemStack.isSameItemSameComponents(visibleStable, liveStable);
    }

    @Unique
    private static void jeg$stripVolatileGunRenderComponents(ItemStack stack) {
        stack.remove(DataComponents.DAMAGE);
        stack.remove(ModDataComponents.GUN_AMMO.get());
        stack.remove(ModDataComponents.GUN_HEAT.get());
        stack.remove(ModDataComponents.GUN_TRIGGER_LOCK.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get());
    }
}
