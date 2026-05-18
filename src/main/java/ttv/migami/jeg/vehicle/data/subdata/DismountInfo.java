package ttv.migami.jeg.vehicle.data.subdata;

public record DismountInfo(double x, double y, double z) {
    public static final DismountInfo DEFAULT = new DismountInfo(0.0D, 0.1D, 0.0D);
}
