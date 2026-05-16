package ttv.migami.jeg.vehicle.data;

public record VehiclePartArmorProfile(
        float rating,
        float undermatchMultiplier,
        float overmatchMultiplier,
        float passengerLeakMultiplier,
        float partDamageMultiplier
) {
    public static final VehiclePartArmorProfile LIGHT = new VehiclePartArmorProfile(2.0F, 0.25F, 1.0F, 0.2F, 1.0F);
}
