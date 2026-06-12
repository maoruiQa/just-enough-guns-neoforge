package ttv.migami.jeg.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.GunItem;

public final class GunMouseSensitivityHandler {
    private static final float BOLT_ACTION_ADS_SENSITIVITY = 0.2F;
    private static final double SENSITIVITY_SCALE = 0.6000000238418579D;
    private static final double SENSITIVITY_OFFSET = 0.20000000298023224D;

    private GunMouseSensitivityHandler() {}

    public static double adjustRawOptionSensitivity(double sensitivity) {
        float ads = boltActionAdsProgress();
        if (ads <= 0.0F) {
            return sensitivity;
        }

        double factor = sensitivityFactor(ads);
        double base = sensitivity * SENSITIVITY_SCALE + SENSITIVITY_OFFSET;
        double adjustedBase = base * Math.cbrt(factor);
        return Mth.clamp((adjustedBase - SENSITIVITY_OFFSET) / SENSITIVITY_SCALE, 0.0D, 1.0D);
    }

    public static double adjustFinalMouseSensitivity(double sensitivity) {
        float ads = boltActionAdsProgress();
        return ads > 0.0F ? sensitivity * sensitivityFactor(ads) : sensitivity;
    }

    private static double sensitivityFactor(float ads) {
        return Mth.lerp(ads, 1.0F, BOLT_ACTION_ADS_SENSITIVITY);
    }

    private static float boltActionAdsProgress() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            return 0.0F;
        }
        if (!(player.getMainHandItem().getItem() instanceof GunItem)
                || !GunScopeSupport.hasTelescopicSight(player.getMainHandItem())) {
            return 0.0F;
        }
        return AimingHandler.get().getNormalisedAdsProgress();
    }
}
