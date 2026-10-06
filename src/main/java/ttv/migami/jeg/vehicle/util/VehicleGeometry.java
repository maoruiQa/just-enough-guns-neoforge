package ttv.migami.jeg.vehicle.util;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector4d;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

import java.util.ArrayList;
import java.util.List;

/** World-space vehicle part geometry. The Minecraft entity AABB remains the block-movement box. */
public final class VehicleGeometry {
    private VehicleGeometry() {}

    public record Hit(Vec3 position, OBBInfo.Part part, double distanceSqr) {}

    private record Box(Vec3 center, Vec3[] axes, double[] half, OBBInfo.Part part) {
        Box moved(Vec3 delta) {
            return new Box(center.add(delta), axes, half, part);
        }

        static Box fromAabb(AABB aabb) {
            return new Box(aabb.getCenter(), new Vec3[]{new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)},
                    new double[]{aabb.getXsize() / 2, aabb.getYsize() / 2, aabb.getZsize() / 2}, OBBInfo.Part.BODY);
        }

        AABB bounds() {
            double x = Math.abs(axes[0].x) * half[0] + Math.abs(axes[1].x) * half[1] + Math.abs(axes[2].x) * half[2];
            double y = Math.abs(axes[0].y) * half[0] + Math.abs(axes[1].y) * half[1] + Math.abs(axes[2].y) * half[2];
            double z = Math.abs(axes[0].z) * half[0] + Math.abs(axes[1].z) * half[1] + Math.abs(axes[2].z) * half[2];
            return new AABB(center.x - x, center.y - y, center.z - z, center.x + x, center.y + y, center.z + z);
        }

        double clip(Vec3 start, Vec3 end) {
            Vec3 from = start.subtract(center);
            Vec3 ray = end.subtract(start);
            double near = 0.0D;
            double far = 1.0D;
            for (int i = 0; i < 3; i++) {
                double origin = from.dot(axes[i]);
                double direction = ray.dot(axes[i]);
                if (Math.abs(direction) < 1.0E-10D) {
                    if (Math.abs(origin) > half[i]) return Double.NaN;
                    continue;
                }
                double t1 = (-half[i] - origin) / direction;
                double t2 = (half[i] - origin) / direction;
                near = Math.max(near, Math.min(t1, t2));
                far = Math.min(far, Math.max(t1, t2));
                if (near > far) return Double.NaN;
            }
            return near;
        }

        boolean intersects(Box other) {
            Vec3 delta = other.center.subtract(center);
            for (Vec3 axis : axes) if (separates(other, delta, axis)) return false;
            for (Vec3 axis : other.axes) if (separates(other, delta, axis)) return false;
            for (Vec3 first : axes) for (Vec3 second : other.axes) {
                Vec3 axis = first.cross(second);
                if (axis.lengthSqr() > 1.0E-12D && separates(other, delta, axis.normalize())) return false;
            }
            return true;
        }

        Vec3 correction(Box other) {
            Vec3 delta = other.center.subtract(center);
            Vec3 best = null;
            double bestDepth = Double.POSITIVE_INFINITY;
            List<Vec3> candidates = new ArrayList<>(15);
            for (Vec3 axis : axes) candidates.add(axis);
            for (Vec3 axis : other.axes) candidates.add(axis);
            for (Vec3 first : axes) for (Vec3 second : other.axes) {
                Vec3 axis = first.cross(second);
                if (axis.lengthSqr() > 1.0E-12D) candidates.add(axis.normalize());
            }
            for (Vec3 axis : candidates) {
                double radius = 0.0D;
                for (int i = 0; i < 3; i++) radius += half[i] * Math.abs(axes[i].dot(axis)) + other.half[i] * Math.abs(other.axes[i].dot(axis));
                double signedDistance = delta.dot(axis);
                double depth = radius - Math.abs(signedDistance);
                if (depth <= 0.0D) return null;
                if (depth < bestDepth) {
                    bestDepth = depth;
                    best = axis.scale((signedDistance >= 0.0D ? 1.0D : -1.0D) * (depth + 1.0E-4D));
                }
            }
            return best;
        }

        private boolean separates(Box other, Vec3 delta, Vec3 axis) {
            double radius = 0.0D;
            for (int i = 0; i < 3; i++) radius += half[i] * Math.abs(axes[i].dot(axis)) + other.half[i] * Math.abs(other.axes[i].dot(axis));
            return Math.abs(delta.dot(axis)) > radius + 1.0E-8D;
        }
    }

    public static AABB bounds(VehicleEntity vehicle) {
        List<Box> boxes = boxes(vehicle);
        if (boxes.isEmpty()) return vehicle.getBoundingBox();
        AABB result = boxes.getFirst().bounds();
        for (int i = 1; i < boxes.size(); i++) result = result.minmax(boxes.get(i).bounds());
        return result;
    }

    public static List<Hit> parts(VehicleEntity vehicle) {
        return boxes(vehicle).stream().map(box -> new Hit(box.center(), box.part(), 0.0D)).toList();
    }

    public static Hit clip(VehicleEntity vehicle, Vec3 start, Vec3 end) {
        return clip(boxes(vehicle), start, end);
    }

    private static Hit clip(List<Box> boxes, Vec3 start, Vec3 end) {
        Hit nearest = null;
        Vec3 ray = end.subtract(start);
        for (Box box : boxes) {
            double t = box.clip(start, end);
            if (Double.isNaN(t)) continue;
            Vec3 position = start.add(ray.scale(t));
            double distance = start.distanceToSqr(position);
            if (nearest == null || distance < nearest.distanceSqr()) nearest = new Hit(position, box.part(), distance);
        }
        return nearest;
    }

    public static boolean intersects(VehicleEntity vehicle, AABB other) {
        Box target = Box.fromAabb(other);
        for (Box box : boxes(vehicle)) if (box.intersects(target)) return true;
        return false;
    }

    public static boolean intersects(VehicleEntity first, VehicleEntity second) {
        List<Box> other = boxes(second);
        for (Box box : boxes(first)) for (Box target : other) if (box.intersects(target)) return true;
        return false;
    }

    /** Minimum translation that moves the other shape away from the vehicle. */
    public static Vec3 correction(VehicleEntity vehicle, AABB other) {
        return correction(boxes(vehicle), List.of(Box.fromAabb(other)));
    }

    /** Minimum translation that moves the second vehicle away from the first. */
    public static Vec3 correction(VehicleEntity first, VehicleEntity second) {
        return correction(boxes(first), boxes(second));
    }

    private static Vec3 correction(List<Box> first, List<Box> second) {
        Vec3 total = Vec3.ZERO;
        for (int pass = 0; pass < 16; pass++) {
            Vec3 step = null;
            for (Box box : first) for (Box target : second) {
                Vec3 movement = box.correction(target.moved(total));
                if (movement != null && (step == null || movement.lengthSqr() > step.lengthSqr())) step = movement;
            }
            if (step == null) return pass == 0 ? null : total;
            total = total.add(step);
        }
        return total;
    }

    private static List<Box> boxes(VehicleEntity vehicle) {
        List<Box> result = new ArrayList<>();
        if (OBBInfo.DEFAULT.equals(vehicle.vehicleData().defaults().obb())) {
            result.add(Box.fromAabb(vehicle.getBoundingBox()));
            return result;
        }
        var source = vehicle.vehicleData().defaults().obb().boxes();
        if (source.isEmpty()) {
            result.add(Box.fromAabb(vehicle.getBoundingBox()));
            return result;
        }
        for (OBBInfo.Box part : source) {
            Matrix4d transform = partTransform(vehicle, part.transform());
            Matrix4d rotation = partTransform(vehicle, part.rotation());
            Vec3 center = point(transform, part.x(), part.y(), part.z(), 1.0D);
            Vec3[] axes = {point(rotation, 1, 0, 0, 0).normalize(), point(rotation, 0, 1, 0, 0).normalize(), point(rotation, 0, 0, 1, 0).normalize()};
            result.add(new Box(center, axes, new double[]{part.halfWidth(), part.halfHeight(), part.halfDepth()}, part.part()));
        }
        return result;
    }

    private static Matrix4d partTransform(VehicleEntity vehicle, String part) {
        Matrix4d transform = bodyTransform(vehicle);
        var turret = vehicle.vehicleData().defaults().turret();
        if (!turret.enabled() || !("turret".equalsIgnoreCase(part) || "barrel".equalsIgnoreCase(part))) return transform;
        transform.translate(turret.originX(), turret.originY(), turret.originZ());
        transform.rotateY(Math.toRadians(vehicle.turretYaw()));
        if ("barrel".equalsIgnoreCase(part)) {
            transform.translate(turret.barrelX(), turret.barrelY(), turret.barrelZ());
            float yaw = vehicle.turretYaw();
            float pitchWeight = (Math.abs(yaw) - 90.0F) / 90.0F;
            float rollWeight = Math.abs(yaw) <= 90.0F ? yaw / 90.0F
                    : yaw < 0.0F ? -(180.0F + yaw) / 90.0F : (180.0F - yaw) / 90.0F;
            transform.rotateX(Math.toRadians(vehicle.turretPitch() + pitchWeight * vehicle.getXRot() + rollWeight * vehicle.roll()));
        }
        return transform;
    }

    private static Matrix4d bodyTransform(VehicleEntity vehicle) {
        return bodyTransform(vehicle.getX(), vehicle.getY(), vehicle.getZ(), vehicle.rotateOffsetHeight(),
                vehicle.getYRot(), vehicle.getXRot(), vehicle.roll());
    }

    private static Matrix4d bodyTransform(double x, double y, double z, double pivot, double yaw, double pitch, double roll) {
        return new Matrix4d().translate(x, y + pivot, z)
                .rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch))
                .rotateZ(Math.toRadians(roll)).translate(0, -pivot, 0);
    }

    private static Vec3 point(Matrix4d matrix, double x, double y, double z, double w) {
        Vector4d p = matrix.transform(new Vector4d(x, y, z, w));
        return new Vec3(p.x, p.y, p.z);
    }

    /** Run with -ea to check the ray and contact math without starting Minecraft. */
    public static void main(String[] args) {
        Vec3[] identity = {new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)};
        Box left = new Box(new Vec3(-2, 0, 0), identity, new double[]{0.5, 1, 1}, OBBInfo.Part.BODY);
        Box right = new Box(new Vec3(2, 0, 0), identity, new double[]{0.5, 1, 1}, OBBInfo.Part.BODY);
        assert clip(List.of(left, right), new Vec3(0, 0, -4), new Vec3(0, 0, 4)) == null;
        Hit leftHit = clip(List.of(left, right), new Vec3(-2, 0, -4), new Vec3(-2, 0, 4));
        assert leftHit != null && leftHit.part() == OBBInfo.Part.BODY && Math.abs(leftHit.position().z + 1.0D) < 1.0E-8D;
        Vec3[] turned = {new Vec3(0, 0, 1), new Vec3(0, 1, 0), new Vec3(-1, 0, 0)};
        Box rotated = new Box(Vec3.ZERO, turned, new double[]{2, 1, 0.25}, OBBInfo.Part.TURRET);
        Hit turnedHit = clip(List.of(rotated), new Vec3(0, 0, -4), new Vec3(0, 0, 4));
        assert turnedHit != null && turnedHit.part() == OBBInfo.Part.TURRET && Math.abs(turnedHit.position().z + 2.0D) < 1.0E-8D;
        assert rotated.intersects(Box.fromAabb(new AABB(-0.2, -0.2, 1.8, 0.2, 0.2, 2.2)));
        assert !rotated.intersects(Box.fromAabb(new AABB(1.8, -0.2, -0.2, 2.2, 0.2, 0.2)));
        Vec3 shift = correction(List.of(rotated), List.of(Box.fromAabb(new AABB(-0.2, -0.2, 1.7, 0.2, 0.2, 2.1))));
        assert shift != null && shift.lengthSqr() > 0.0D;
        assert correction(List.of(rotated), List.of(Box.fromAabb(new AABB(1.8, -0.2, -0.2, 2.2, 0.2, 0.2)))) == null;
        Matrix4d yawed = bodyTransform(0, 0, 0, 0, 90, 0, 0);
        assert point(yawed, 1, 0, 0, 0).distanceTo(new Vec3(0, 0, 1)) < 1.0E-8D;
        assert point(yawed, 0, 0, 1, 1).distanceTo(new Vec3(-1, 0, 0)) < 1.0E-8D;
        Matrix4d tilted = bodyTransform(0, 0, 0, 1, 0, 90, 90);
        assert point(tilted, 0, 1, 0, 1).distanceTo(new Vec3(0, 1, 0)) < 1.0E-8D;
        Vec3 firstAxis = point(tilted, 1, 0, 0, 0);
        Vec3 secondAxis = point(tilted, 0, 1, 0, 0);
        assert Math.abs(firstAxis.lengthSqr() - 1) < 1.0E-8D && Math.abs(firstAxis.dot(secondAxis)) < 1.0E-8D;
    }
}
