package ttv.migami.jeg.client.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.GunItem;

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

    private AimingHandler() {}

    public static AimingHandler get() {
        return INSTANCE;
    }

    public void tick(LocalPlayer player) {
        previousAim = currentAim;

        if (shouldAim(player)) {
            currentAim = Math.min(MAX_AIM_PROGRESS, currentAim + AIM_SPEED);
        } else {
            currentAim = Math.max(0.0F, currentAim - AIM_SPEED);
        }
    }

    public boolean isAiming() {
        return currentAim > 0.0F || previousAim > 0.0F;
    }

    public void reset() {
        currentAim = 0.0F;
        previousAim = 0.0F;
    }

    public float getNormalisedAdsProgress(float partialTick) {
        float progress = Mth.lerp(partialTick, previousAim, currentAim) / MAX_AIM_PROGRESS;
        return Mth.clamp(progress, 0.0F, 1.0F);
    }

    public float getNormalisedAdsProgress() {
        return getNormalisedAdsProgress(1.0F);
    }

    private static boolean shouldAim(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.screen != null || player.isSpectator()) {
            return false;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof GunItem)) {
            return false;
        }

        return minecraft.options.keyUse.isDown();
    }
}
