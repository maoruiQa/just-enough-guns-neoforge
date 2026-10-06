package ttv.migami.jeg.vehicle.util;

import ttv.migami.jeg.vehicle.data.subdata.OBBInfo;

/** The narrow-phase result carried by vanilla projectiles until their damage call. */
public interface VehiclePartHit {
    void jeg$setVehiclePartHit(int entityId, OBBInfo.Part part);
    OBBInfo.Part jeg$getVehiclePartHit(int entityId);
}
