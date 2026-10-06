package ttv.migami.jeg.vehicle.data.subdata;

public record DestroyInfo(boolean explodes, float explosionPower, float explosionDamage, boolean explodeBlocks,
                          boolean explodePassengers, boolean crashPassengers, String particleType) {
    public static final DestroyInfo NONE = new DestroyInfo(false, 0.0F);

    public DestroyInfo(boolean explodes, float explosionPower) {
        this(explodes, explosionPower, 0.0F, true, false, false, "mini");
    }
}
