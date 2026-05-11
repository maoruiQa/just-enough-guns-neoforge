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
        float sensitivityZ
) {
    public static final SeatInfo DRIVER = new SeatInfo(0, 0.0D, 0.65D, 0.0D, true, false, false, false, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F);

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger) {
        this(index, x, y, z, driver, enclosed, hidePassenger, false, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, -90.0F, 90.0F, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch) {
        this(index, x, y, z, driver, enclosed, hidePassenger, banHand, minPitch, maxPitch, -180.0F, 180.0F, 1.0F, 1.0F, 1.0F);
    }

    public SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed, boolean hidePassenger, boolean banHand, float minPitch, float maxPitch, float minYaw, float maxYaw, float sensitivityX, float sensitivityY, float sensitivityZ) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.z = z;
        this.driver = driver;
        this.enclosed = enclosed;
        this.hidePassenger = hidePassenger;
        this.banHand = banHand;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.minYaw = minYaw;
        this.maxYaw = maxYaw;
        this.sensitivityX = sensitivityX;
        this.sensitivityY = sensitivityY;
        this.sensitivityZ = sensitivityZ;
    }
}
