package ttv.migami.jeg.vehicle.data.subdata;

public record CameraPos(double x, double y, double z, double zoomX, double zoomY, double zoomZ) {
    public static final CameraPos DEFAULT = new CameraPos(0.0D, 2.4D, -5.0D, 0.0D, 0.0D, 0.0D);

    public CameraPos(double x, double y, double z) {
        this(x, y, z, 0.0D, 0.0D, 0.0D);
    }
}
