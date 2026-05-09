package ttv.migami.jeg.vehicle.data.subdata;

public enum VehicleContainerType {
    NONE(0, 0),
    MINI(1, 9),
    SMALL(3, 9),
    MEDIUM(6, 9),
    LARGE(6, 13),
    HUGE(6, 17);

    private final int rows;
    private final int columns;

    VehicleContainerType(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public int rows() {
        return this.rows;
    }

    public int columns() {
        return this.columns;
    }

    public int slots() {
        return this.rows * this.columns;
    }
}
