package ttv.migami.jeg.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.item.attachment.GunAttachments;

/**
 * Lightweight ADS tracker for first-person render/FOV transforms.
 * Uses right-click hold semantics like 1.20.1.
 */
public final class AimingHandler {
    private static final float MAX_AIM_PROGRESS = 5.0F;
    private static final float AIM_SPEED = 1.0F;

    private static final AimingHandler INSTANCE = new AimingHandler();

    private float currentAim;
    private float previousAim;
    private boolean suppressedUntilUseReleased;

    private AimingHandler() {}

    public static AimingHandler get() {
        return INSTANCE;
    }

    public void tick(LocalPlayer player) {
        if (GunItem.isDrawing(player.getMainHandItem())) {
            currentAim = 0.0F;
            previousAim = 0.0F;
            return;
        }
        previousAim = currentAim;
        float aimSpeed = aimSpeed(player);

        if (shouldAim(player)) {
            currentAim = Math.min(MAX_AIM_PROGRESS, currentAim + aimSpeed);
        } else {
            currentAim = Math.max(0.0F, currentAim - aimSpeed);
        }
    }

    public boolean isAiming() {
        return currentAim > 0.0F || previousAim > 0.0F;
    }

    public void reset() {
        currentAim = 0.0F;
        previousAim = 0.0F;
        suppressedUntilUseReleased = false;
    }

    public void suppressUntilUseReleased() {
        currentAim = 0.0F;
        previousAim = 0.0F;
        suppressedUntilUseReleased = true;
    }

    public float getNormalisedAdsProgress(float partialTick) {
        float progress = Mth.lerp(partialTick, previousAim, currentAim) / MAX_AIM_PROGRESS;
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public float getNormalisedAdsProgress() {
        return getNormalisedAdsProgress(1.0F);
    }

    private boolean shouldAim(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.gui.screen() != null || player.isSpectator()) {
            return false;
        }
        if (suppressedUntilUseReleased) {
            if (minecraft.options.keyUse.isDown()) {
                return false;
            }
            suppressedUntilUseReleased = false;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof GunItem) || GunItem.isDrawing(mainHand)) {
            return false;
        }

        return minecraft.options.keyUse.isDown();
    }

    private static float aimSpeed(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof GunItem)) {
            return AIM_SPEED;
        }
        return Math.max(0.05F, AIM_SPEED * (float) GunAttachments.modifiers(mainHand).adsSpeedMultiplier());
    }
}
