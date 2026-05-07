package ttv.migami.jeg.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    private ItemStack jeg$currentFirstPersonStack = ItemStack.EMPTY;

    @Unique
    private float jeg$capturedEquipProcess = Float.NaN;

    @Unique
    private float jeg$capturedSwingProcess = Float.NaN;

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
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        this.jeg$currentFirstPersonStack = stack;
        this.jeg$capturedEquipProcess = equipProgress;
        this.jeg$capturedSwingProcess = swingProgress;
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
        this.jeg$currentFirstPersonStack = ItemStack.EMPTY;
        this.jeg$capturedEquipProcess = Float.NaN;
        this.jeg$capturedSwingProcess = Float.NaN;
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
            // Set to 1.0F (fully equipped) instead of 0.0F to avoid equip animation depression
            // equipProgress = 1.0F - mainHandHeight, so mainHandHeight=1.0F → equipProgress=0.0F
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
    private void jeg$applyGunHandTransform(
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
            ttv.migami.jeg.client.GunHandTransform.apply(
                    poseStack,
                    player,
                    arm,
                    gun.getStats(),
                    partialTick
            );
        }
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
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_END_TICK.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_WATER_COOLING_TICKS_REMAINING.get());
    }

}
