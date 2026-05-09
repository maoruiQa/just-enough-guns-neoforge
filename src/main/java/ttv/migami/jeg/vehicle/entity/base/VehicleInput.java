package ttv.migami.jeg.vehicle.entity.base;

public record VehicleInput(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean brake,
        boolean fire,
        boolean freeLook
) {
    public static final VehicleInput EMPTY = new VehicleInput(false, false, false, false, false, false, false);

    public int forwardAxis() {
        return (this.forward ? 1 : 0) - (this.backward ? 1 : 0);
    }

    public int strafeAxis() {
        return (this.left ? 1 : 0) - (this.right ? 1 : 0);
    }
}
