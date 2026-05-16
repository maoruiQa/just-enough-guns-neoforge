package ttv.migami.jeg.vehicle.data.subdata;

public record CameraPos(
        double x,
        double y,
        double z,
        double zoomX,
        double zoomY,
        double zoomZ,
        boolean useFixedCameraPos,
        boolean useAircraftCamera,
        double aircraftX,
        double aircraftY,
        double aircraftZ,
        boolean useSimulatedThirdPerson,
        double simulatedThirdPersonDistance,
        double simulatedThirdPersonHeight
) {
    public static final CameraPos DEFAULT = new CameraPos(0.0D, 2.4D, -5.0D, 0.0D, 0.0D, 0.0D, false, false, 0.0D, 3.0D, -10.0D, false, 0.0D, 0.0D);

    public CameraPos(double x, double y, double z) {
        this(x, y, z, 0.0D, 0.0D, 0.0D);
    }

    public CameraPos(double x, double y, double z, double zoomX, double zoomY, double zoomZ) {
        this(x, y, z, zoomX, zoomY, zoomZ, false, false, 0.0D, 3.0D, -10.0D, false, 0.0D, 0.0D);
    }
}
