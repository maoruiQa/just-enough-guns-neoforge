package ttv.migami.jeg.vehicle.data.subdata;

public record TurretInfo(
        int seatIndex,
        double renderPivotY,
        double originX,
        double originY,
        double originZ,
        double barrelX,
        double barrelY,
        double barrelZ,
        boolean guidedUsesTurret
) {
    public static final TurretInfo NONE = new TurretInfo(-1, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false);

    public boolean enabled() {
        return this.seatIndex >= 0;
    }
}
