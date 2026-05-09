package ttv.migami.jeg.vehicle.data.subdata;

public enum VehicleContainerType {
    NONE(0, 0),
    MINI(1, 9),
    SMALL(2, 18),
    MEDIUM(3, 27),
    LARGE(4, 36),
    HUGE(6, 54);

    private final int rows;
    private final int slots;

    VehicleContainerType(int rows, int slots) {
        this.rows = rows;
        this.slots = slots;
    }

    public int rows() {
        return this.rows;
    }

    public int slots() {
        return this.slots;
    }
}
