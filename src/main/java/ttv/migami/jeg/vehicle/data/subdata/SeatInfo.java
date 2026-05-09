package ttv.migami.jeg.vehicle.data.subdata;

public record SeatInfo(int index, double x, double y, double z, boolean driver, boolean enclosed) {
    public static final SeatInfo DRIVER = new SeatInfo(0, 0.0D, 0.65D, 0.0D, true, false);
}
