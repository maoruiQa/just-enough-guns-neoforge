package ttv.migami.jeg.vehicle.entity.base;

public record VehicleInput(
        boolean forward,
        boolean backward,
        boolean left,
        boolean right,
        boolean brake,
        boolean ascend,
        boolean descend,
        boolean fire,
        boolean freeLook,
        boolean switchWeapon,
        boolean previousWeapon,
        boolean seekTarget,
        boolean deployDecoy
) {
    public static final VehicleInput EMPTY = new VehicleInput(false, false, false, false, false, false, false, false, false, false, false, false, false);

    public int forwardAxis() {
        return (this.forward ? 1 : 0) - (this.backward ? 1 : 0);
    }

    public int strafeAxis() {
        return (this.left ? 1 : 0) - (this.right ? 1 : 0);
    }

    public int verticalAxis() {
        return (this.ascend ? 1 : 0) - (this.descend ? 1 : 0);
    }
}
