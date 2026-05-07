package ttv.migami.jeg.client;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import ttv.migami.jeg.client.handler.AimingHandler;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.gun.RecoilProfiles;

/**
 * Keeps 1.20.1-style camera recoil and first-person gun model recoil in sync.
 */
public final class GunRecoilHandler {
    private static final Random RANDOM = new Random();
    private static final long SPRINT_POSE_SUPPRESSION_NANOS = 250_000_000L;

    private static float recoil;
    private static float previous;
    private static double gunRecoilNormal;
    private static double gunRecoilAngle;
    private static float gunRecoilKick;
    private static float gunRecoilDurationOffset;
    private static float gunRecoilAdsReduction = 0.2F;
    private static float gunRecoilRandom;
    private static float cameraRecoil;
    private static float progressCameraRecoil;
    private static int cameraRecoilDirection = 1;
    private static int gunRecoilTicks;
    private static int gunRecoilDuration;
    private static long suppressSprintPoseUntilNanos;

    private GunRecoilHandler() {}

    public static void tick() {
        previous = recoil;
        tickCameraRecoil();
        tickGunRecoil();
        recoil = (float) gunRecoilNormal;
    }

    public static void onShot(GunStats stats) {
        RecoilProfiles.Parameters parameters = RecoilProfiles.parameters(stats.id());
        float adsReduction = getAdsRecoilReduction(parameters);
        cameraRecoil = parameters.angle() * adsReduction;
        progressCameraRecoil = 0.0F;
        cameraRecoilDirection = RANDOM.nextBoolean() ? 1 : -1;
        gunRecoilNormal = parameters.angle() > 0.0F || parameters.kick() > 0.0F ? 1.0D : 0.0D;
        gunRecoilAngle = parameters.angle();
        gunRecoilKick = parameters.kick();
        gunRecoilDurationOffset = parameters.durationOffset();
        gunRecoilAdsReduction = parameters.adsReduction();
        gunRecoilRandom = RANDOM.nextFloat();
        gunRecoilTicks = 0;
        gunRecoilDuration = Math.max(4, stats.fireDelay());
        recoil = (float) gunRecoilNormal;
        previous = recoil;
        suppressSprintPoseUntilNanos = System.nanoTime() + SPRINT_POSE_SUPPRESSION_NANOS;
    }

    public static void addShot(float amount) {
        cameraRecoil = Math.max(0.0F, amount);
        progressCameraRecoil = 0.0F;
        cameraRecoilDirection = RANDOM.nextBoolean() ? 1 : -1;
    }

    public static void addDryFire(float amount) {
        cameraRecoil = Math.max(cameraRecoil, amount * 0.35F);
        progressCameraRecoil = 0.0F;
        cameraRecoilDirection = RANDOM.nextBoolean() ? 1 : -1;
        gunRecoilNormal = Math.max(gunRecoilNormal, 0.25D);
        gunRecoilAngle = Math.max(gunRecoilAngle, amount);
        gunRecoilKick = 0.0F;
        gunRecoilRandom = RANDOM.nextFloat();
        gunRecoilTicks = 0;
        gunRecoilDuration = 4;
        recoil = (float) gunRecoilNormal;
        previous = recoil;
    }

    private static void tickCameraRecoil() {
        if (cameraRecoil <= 0.0F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            cameraRecoil = 0.0F;
            progressCameraRecoil = 0.0F;
            return;
        }

        float recoilAmount = cameraRecoil * 0.15F;
        float startProgress = progressCameraRecoil / cameraRecoil;
        float endProgress = Math.min(1.0F, (progressCameraRecoil + recoilAmount) / cameraRecoil);
        float delta = endProgress - startProgress;
        float pitch = minecraft.player.getXRot();
        float yaw = minecraft.player.getYRot();

        if (startProgress < 0.2F) {
            pitch -= (delta / 0.2F) * cameraRecoil;
            yaw += cameraRecoilDirection * (delta / 0.2F) * cameraRecoil * 0.5F;
        } else {
            pitch += (delta / 0.8F) * cameraRecoil;
            yaw -= cameraRecoilDirection * (delta / 0.8F) * cameraRecoil * 0.5F;
        }

        minecraft.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        minecraft.player.setYRot(yaw);
        progressCameraRecoil += recoilAmount;

        if (progressCameraRecoil >= cameraRecoil) {
            cameraRecoil = 0.0F;
            progressCameraRecoil = 0.0F;
        }
    }

    private static void tickGunRecoil() {
        if (gunRecoilDuration <= 0) {
            gunRecoilNormal = 0.0D;
            return;
        }

        gunRecoilTicks++;
        float cooldown = 1.0F - Mth.clamp((float) gunRecoilTicks / (float) gunRecoilDuration, 0.0F, 1.0F);
        float offset = Mth.clamp(gunRecoilDurationOffset, 0.0F, 0.95F);
        cooldown = cooldown >= offset ? (cooldown - offset) / (1.0F - offset) : 0.0F;
        if (cooldown >= 0.8F) {
            float amount = (1.0F - cooldown) / 0.2F;
            gunRecoilNormal = 1.0D - (--amount) * amount * amount * amount;
        } else {
            float amount = cooldown / 0.8F;
            gunRecoilNormal = amount < 0.5F ? 2.0F * amount * amount : -1.0F + (4.0F - 2.0F * amount) * amount;
        }

        if (gunRecoilTicks >= gunRecoilDuration) {
            gunRecoilDuration = 0;
            gunRecoilTicks = 0;
            gunRecoilNormal = 0.0D;
        }
    }

    public static void stopImmediate() {
        recoil = 0.0F;
        previous = 0.0F;
        gunRecoilNormal = 0.0D;
        gunRecoilAngle = 0.0D;
        gunRecoilKick = 0.0F;
        cameraRecoil = 0.0F;
        progressCameraRecoil = 0.0F;
        gunRecoilTicks = 0;
        gunRecoilDuration = 0;
    }

    public static float getRecoil(float partialTick) {
        return Mth.lerp(partialTick, previous, recoil);
    }

    public static boolean isSuppressingSprintPose() {
        return System.nanoTime() < suppressSprintPoseUntilNanos;
    }

    public static float current() {
        return recoil;
    }

    public static double getGunRecoilNormal() {
        return gunRecoilNormal;
    }

    public static double getGunRecoilAngle() {
        return gunRecoilAngle;
    }

    public static float getGunRecoilKick() {
        return gunRecoilKick;
    }

    public static float getGunRecoilRandom() {
        return gunRecoilRandom;
    }

    public static float getAdsRecoilReduction() {
        return 1.0F - gunRecoilAdsReduction * AimingHandler.get().getNormalisedAdsProgress();
    }

    private static float getAdsRecoilReduction(RecoilProfiles.Parameters parameters) {
        return 1.0F - parameters.adsReduction() * AimingHandler.get().getNormalisedAdsProgress();
    }
}
