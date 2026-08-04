package ttv.migami.jeg.vehicle.data.subdata;

public record SeekInfo(double range, double angle, boolean warnsTarget, int time) {
    public static final SeekInfo NONE = new SeekInfo(0.0D, 0.0D, false, 0);

    public SeekInfo(double range, double angle, boolean warnsTarget) {
        this(range, angle, warnsTarget, 0);
    }
}
