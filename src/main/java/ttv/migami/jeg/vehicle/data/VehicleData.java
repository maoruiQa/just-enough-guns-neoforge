package ttv.migami.jeg.vehicle.data;

import net.minecraft.resources.ResourceLocation;

public final class VehicleData {
    private final DefaultVehicleData defaults;

    public VehicleData(DefaultVehicleData defaults) {
        this.defaults = defaults;
    }

    public ResourceLocation id() {
        return this.defaults.id();
    }

    public DefaultVehicleData defaults() {
        return this.defaults;
    }
}
