package ttv.migami.jeg.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.GunItem;

public final class GunMouseSensitivityHandler {
    private static final float ADS_SENSITIVITY = 0.8F;
    private static final float TELESCOPIC_ADS_SENSITIVITY = 0.2F;

    private GunMouseSensitivityHandler() {}

    public static double adjustFinalMouseSensitivity(double sensitivity) {
        float ads = adsProgress();
        return ads > 0.0F ? sensitivity * Mth.lerp(ads, 1.0F, telescopicSight() ? TELESCOPIC_ADS_SENSITIVITY : ADS_SENSITIVITY) : sensitivity;
    }

    private static boolean telescopicSight() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null && GunScopeSupport.hasTelescopicSight(player.getMainHandItem());
    }

    private static float adsProgress() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return 0.0F;
        }
        if (!(player.getMainHandItem().getItem() instanceof GunItem)) {
            return 0.0F;
        }
        return AimingHandler.get().getNormalisedAdsProgress();
    }
}
