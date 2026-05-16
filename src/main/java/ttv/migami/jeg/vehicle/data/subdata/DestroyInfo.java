package ttv.migami.jeg.vehicle.data.subdata;

public record DestroyInfo(boolean explodes, float explosionPower) {
    public static final DestroyInfo NONE = new DestroyInfo(false, 0.0F);
}
