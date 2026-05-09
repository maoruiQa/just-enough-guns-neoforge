package ttv.migami.jeg.vehicle.data.subdata;

public record CameraPos(double x, double y, double z) {
    public static final CameraPos DEFAULT = new CameraPos(0.0D, 2.4D, -5.0D);
}
