package ttv.migami.jeg.client;

import net.minecraft.util.Mth;

/**
 * Maintains a small recoil curve that item rendering can query to add subtle kickback.
 */
public final class GunRecoilHandler {
    private static float recoil;
    private static float previous;

    private GunRecoilHandler() {}

    public static void tick() {
        previous = recoil;
        recoil = Mth.approach(recoil, 0.0F, 0.08F);
    }

    public static void addShot(float amount) {
        recoil = Mth.clamp(recoil + amount, 0.0F, 1.5F);
    }

    public static void addDryFire(float amount) {
        recoil = Mth.clamp(recoil + amount, 0.0F, 0.4F);
    }

    public static float getRecoil(float partialTick) {
        return Mth.lerp(partialTick, previous, recoil);
    }

    public static float current() {
        return recoil;
    }
}
