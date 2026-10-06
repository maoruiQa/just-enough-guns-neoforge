package ttv.migami.jeg.vehicle.data.subdata;

public record SeatInfo(
        int index,
        double x,
        double y,
        double z,
        boolean driver,
        boolean enclosed,
        boolean hidePassenger,
        boolean banHand,
        float minPitch,
        float maxPitch,
        float minYaw,
        float maxYaw,
        float sensitivityX,
        float sensitivityY,
        float sensitivityZ,
        CameraPos zoomCamera,
        DismountInfo dismount,
        String transform,
        float orientation,
        boolean canRotateHead,
        boolean canRotateBody
) {
    public static final SeatInfo DRIVER = new SeatInfo(0, 0.0D, 0.65D, 0.0D, true, false, false, false, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F, new CameraPos(0.0D, 0.0D, 0.0D), DismountInfo.DEFAULT);

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger) {
        this(index, x, y, z, driver, enclosed, hidePassenger, false, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F, new CameraPos(0.0D, 0.0D, 0.0D), DismountInfo.DEFAULT);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F, new CameraPos(0.0D, 0.0D, 0.0D), DismountInfo.DEFAULT);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, minPitch, maxPitch, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F, new CameraPos(0.0D, 0.0D, 0.0D), DismountInfo.DEFAULT);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch, float minYaw, float maxYaw, float sensitivityX, float sensitivityY, float sensitivityZ) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, minPitch, maxPitch, minYaw, maxYaw, sensitivityX, sensitivityY, sensitivityZ, new CameraPos(0.0D, 0.0D, 0.0D), DismountInfo.DEFAULT);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch, float minYaw, float maxYaw, float sensitivityX, float sensitivityY, float sensitivityZ, CameraPos zoomCamera) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, minPitch, maxPitch, minYaw, maxYaw, sensitivityX, sensitivityY, sensitivityZ, zoomCamera, DismountInfo.DEFAULT);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch, float minYaw, float maxYaw, float sensitivityX, float sensitivityY, float sensitivityZ, CameraPos zoomCamera, DismountInfo dismount) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, minPitch, maxPitch, minYaw, maxYaw, sensitivityX, sensitivityY, sensitivityZ, zoomCamera, dismount, "vehicle", 0.0F, true, true);
    }
}
