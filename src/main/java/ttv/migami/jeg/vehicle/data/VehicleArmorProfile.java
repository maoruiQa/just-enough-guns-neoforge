package ttv.migami.jeg.vehicle.data;

import java.util.EnumMap;
import java.util.Map;
import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;

public final class VehicleArmorProfile {
    public static final VehicleArmorProfile LIGHT = new VehicleArmorProfile(VehiclePartArmorProfile.LIGHT, Map.of());

    private final VehiclePartArmorProfile fallback;
    private final EnumMap<OBBInfo.Part, VehiclePartArmorProfile> parts;

    public VehicleArmorProfile(VehiclePartArmorProfile fallback, Map<OBBInfo.Part, VehiclePartArmorProfile> parts) {
        this.fallback = fallback;
        this.parts = new EnumMap<>(OBBInfo.Part.class);
        this.parts.putAll(parts);
    }

    public VehiclePartArmorProfile forPart(OBBInfo.Part part) {
        return this.parts.getOrDefault(part, this.fallback);
    }

    public VehiclePartArmorProfile fallback() {
        return this.fallback;
    }

    public Map<OBBInfo.Part, VehiclePartArmorProfile> parts() {
        return Map.copyOf(this.parts);
    }
}
