package ttv.migami.jeg.vehicle.data.subdata;

public record EngineInfo(EngineType type, double acceleration, double maxForwardSpeed, double maxReverseSpeed, double friction) {
    public static final EngineInfo WHEEL_TEST = new EngineInfo(EngineType.WHEEL, 0.045D, 0.42D, 0.18D, 0.82D);
}
