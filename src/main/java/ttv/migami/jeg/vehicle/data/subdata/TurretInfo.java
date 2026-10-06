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
        boolean guidedUsesTurret,
        float pitchTurnSpeed,
        float yawTurnSpeed,
        float minPitch,
        float maxPitch
) {
    public TurretInfo(int seatIndex, double renderPivotY, double originX, double originY, double originZ, double barrelX, double barrelY, double barrelZ, boolean guidedUsesTurret) {
        this(seatIndex, renderPivotY, originX, originY, originZ, barrelX, barrelY, barrelZ, guidedUsesTurret, 180.0F, 180.0F, -90.0F, 90.0F);
    }

    public static final TurretInfo NONE = new TurretInfo(-1, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false);

    public boolean enabled() {
        return this.seatIndex >= 0;
    }
}
