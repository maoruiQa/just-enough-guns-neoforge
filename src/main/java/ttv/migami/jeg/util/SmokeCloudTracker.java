package ttv.migami.jeg.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Position-based smoke volumes for lock denial.
 * Entities report every tick on both logical sides so client lock UI and server validation
 * stay in sync without relying on entity hitbox broadphase.
 */
public final class SmokeCloudTracker {
    public static final double HALF_WIDTH = 6.0D;
    public static final double HEIGHT = 8.0D;
    private static final double LOS_STEP = 0.35D;

    private static final Map<Level, List<Cloud>> CLOUDS = new ConcurrentHashMap<>();

    private SmokeCloudTracker() {}

    public static void report(Level level, double x, double y, double z) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        List<Cloud> list = CLOUDS.computeIfAbsent(level, key -> new ArrayList<>());
        synchronized (list) {
            // Merge with nearby existing report (same cloud source).
            for (Cloud cloud : list) {
                if (cloud.matches(x, y, z)) {
                    cloud.expireGameTime = now + 5L;
                    cloud.x = x;
                    cloud.y = y;
                    cloud.z = z;
                    return;
                }
            }
            list.add(new Cloud(x, y, z, now + 5L));
        }
    }

    public static void purgeLevel(Level level) {
        if (level != null) {
            CLOUDS.remove(level);
        }
    }

    public static void tickExpiry(Level level) {
        if (level == null) {
            return;
        }
        List<Cloud> list = CLOUDS.get(level);
        if (list == null) {
            return;
        }
        long now = level.getGameTime();
        synchronized (list) {
            Iterator<Cloud> it = list.iterator();
            while (it.hasNext()) {
                if (it.next().expireGameTime < now) {
                    it.remove();
                }
            }
            if (list.isEmpty()) {
                CLOUDS.remove(level, list);
            }
        }
    }

    public static boolean pointInSmoke(Level level, Vec3 point) {
        if (level == null || point == null) {
            return false;
        }
        tickExpiry(level);
        List<Cloud> list = CLOUDS.get(level);
        if (list == null || list.isEmpty()) {
            return false;
        }
        synchronized (list) {
            for (Cloud cloud : list) {
                if (cloud.contains(point)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean boxIntersectsSmoke(Level level, AABB box) {
        if (level == null || box == null) {
            return false;
        }
        tickExpiry(level);
        List<Cloud> list = CLOUDS.get(level);
        if (list == null || list.isEmpty()) {
            return false;
        }
        synchronized (list) {
            for (Cloud cloud : list) {
                if (cloud.volume().intersects(box)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean lineOccluded(Level level, Vec3 from, Vec3 to) {
        if (level == null || from == null || to == null) {
            return false;
        }
        if (pointInSmoke(level, from) || pointInSmoke(level, to)) {
            return true;
        }
        tickExpiry(level);
        List<Cloud> list = CLOUDS.get(level);
        if (list == null || list.isEmpty()) {
            return false;
        }
        synchronized (list) {
            for (Cloud cloud : list) {
                if (segmentIntersectsAabb(from, to, cloud.volume())) {
                    return true;
                }
            }
        }
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return false;
        }
        int steps = Math.max(1, Mth.ceil(length / LOS_STEP));
        for (int i = 1; i < steps; i++) {
            if (pointInSmoke(level, from.add(delta.scale((double) i / (double) steps)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean segmentIntersectsAabb(Vec3 a, Vec3 b, AABB box) {
        double tMin = 0.0D;
        double tMax = 1.0D;
        double[] tx = slab(a.x, b.x - a.x, box.minX, box.maxX, tMin, tMax);
        if (tx[0] > tx[1]) {
            return false;
        }
        tMin = tx[0];
        tMax = tx[1];
        double[] ty = slab(a.y, b.y - a.y, box.minY, box.maxY, tMin, tMax);
        if (ty[0] > ty[1]) {
            return false;
        }
        tMin = ty[0];
        tMax = ty[1];
        double[] tz = slab(a.z, b.z - a.z, box.minZ, box.maxZ, tMin, tMax);
        return tz[0] <= tz[1];
    }

    private static double[] slab(double start, double dir, double min, double max, double tMin, double tMax) {
        if (Math.abs(dir) < 1.0E-8D) {
            if (start < min || start > max) {
                return new double[] {1.0D, 0.0D};
            }
            return new double[] {tMin, tMax};
        }
        double inv = 1.0D / dir;
        double t1 = (min - start) * inv;
        double t2 = (max - start) * inv;
        if (t1 > t2) {
            double tmp = t1;
            t1 = t2;
            t2 = tmp;
        }
        return new double[] {Math.max(tMin, t1), Math.min(tMax, t2)};
    }

    private static final class Cloud {
        private double x;
        private double y;
        private double z;
        private long expireGameTime;

        private Cloud(double x, double y, double z, long expireGameTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.expireGameTime = expireGameTime;
        }

        private boolean matches(double ox, double oy, double oz) {
            double dx = this.x - ox;
            double dy = this.y - oy;
            double dz = this.z - oz;
            return dx * dx + dy * dy + dz * dz < 1.0D;
        }

        private boolean contains(Vec3 point) {
            return point.x >= this.x - HALF_WIDTH && point.x <= this.x + HALF_WIDTH
                    && point.z >= this.z - HALF_WIDTH && point.z <= this.z + HALF_WIDTH
                    && point.y >= this.y - 0.5D && point.y <= this.y + HEIGHT;
        }

        private AABB volume() {
            return new AABB(
                    this.x - HALF_WIDTH,
                    this.y - 0.5D,
                    this.z - HALF_WIDTH,
                    this.x + HALF_WIDTH,
                    this.y + HEIGHT,
                    this.z + HALF_WIDTH
            );
        }
    }
}
