package ttv.migami.jeg.vehicle.data.subdata;

public record SeekInfo(double range, double angle, boolean warnsTarget) {
    public static final SeekInfo NONE = new SeekInfo(0.0D, 0.0D, false);
}
