package ttv.migami.jeg.vehicle.data.subdata;

import java.util.List;

public record OBBInfo(List<Box> boxes) {
    public static final OBBInfo DEFAULT = new OBBInfo(List.of(new Box(Part.BODY, 0.0D, 0.75D, 0.0D, 0.9D, 0.75D, 1.2D)));

    public enum Part {
        BODY,
        TURRET,
        WHEEL_LEFT,
        WHEEL_RIGHT,
        MAIN_ENGINE,
        SUB_ENGINE,
        INTERACTIVE
    }

    public record Box(Part part, double x, double y, double z, double halfWidth, double halfHeight, double halfDepth) {}
}
