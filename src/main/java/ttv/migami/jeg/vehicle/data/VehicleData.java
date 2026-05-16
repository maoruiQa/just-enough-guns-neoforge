package ttv.migami.jeg.vehicle.data;

import net.minecraft.resources.Identifier;

public final class VehicleData {
    private final DefaultVehicleData defaults;

    public VehicleData(DefaultVehicleData defaults) {
        this.defaults = defaults;
    }

    public Identifier id() {
        return this.defaults.id();
    }

    public DefaultVehicleData defaults() {
        return this.defaults;
    }
}
