package ttv.migami.jeg.compat;

import net.minecraft.world.phys.Vec3;

public final class ClientHooks {
    public interface Impl {
        void addDryFireRecoil(float amount);

        void addShotRecoil(float amount);

        void addBulletTrail(Vec3 start, Vec3 end, int color, float size);
    }

    private static volatile Impl impl;

    private ClientHooks() {
    }

    public static void setImpl(Impl newImpl) {
        impl = newImpl;
    }

    public static void addDryFireRecoil(float amount) {
        Impl current = impl;
        if (current != null) {
            current.addDryFireRecoil(amount);
        }
    }

    public static void addShotRecoil(float amount) {
        Impl current = impl;
        if (current != null) {
            current.addShotRecoil(amount);
        }
    }

    public static void addBulletTrail(Vec3 start, Vec3 end, int color, float size) {
        Impl current = impl;
        if (current != null) {
            current.addBulletTrail(start, end, color, size);
        }
    }
}
