package ttv.migami.jeg.vehicle.data.subdata;

import net.minecraft.resources.Identifier;

public record EngineInfo(
        EngineType type,
        double acceleration,
        double maxForwardSpeed,
        double maxReverseSpeed,
        double friction,
        double steeringSpeed,
        int energyCostRate,
        double increment,
        double decrement,
        double pitchSpeed,
        double yawSpeed,
        double rollSpeed,
        double liftSpeed,
        Identifier engineStartSound,
        float engineSoundVolume,
        boolean swControls,
        double buoyancy,
        double bodyPitchRate,
        double bodyRollRate,
        double mouseSensitivity,
        double mouseSpeedX,
        double mouseSpeedY,
        double wheelRotSpeed,
        double wheelDifferential,
        double trackRotSpeed,
        double trackDifferential,
        double inertiaRotateRate,
        double terrainRotateRate,
        java.util.List<net.minecraft.world.phys.Vec3> terrainPoints
) {
    public EngineInfo(EngineType type, double acceleration, double maxForwardSpeed, double maxReverseSpeed, double friction, double steeringSpeed, int energyCostRate, double increment, double decrement, double pitchSpeed, double yawSpeed, double rollSpeed, double liftSpeed, Identifier engineStartSound, float engineSoundVolume) {
        this(type, acceleration, maxForwardSpeed, maxReverseSpeed, friction, steeringSpeed, energyCostRate, increment, decrement, pitchSpeed, yawSpeed, rollSpeed, liftSpeed, engineStartSound, engineSoundVolume, false, 0.0D, 1.0D, 1.0D, 0.25D, 0.4D, 0.25D, 1.0D, 0.5D, 1.0D, 0.5D, 1.0D, 0.1D, java.util.List.of());
    }

    public static final EngineInfo WHEEL_TEST = new EngineInfo(EngineType.WHEEL, 0.045D, 0.42D, 0.18D, 0.82D, 0.0D, 0, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, null, 1.0F);

    public EngineInfo(EngineType type, double acceleration, double maxForwardSpeed, double maxReverseSpeed, double friction, double steeringSpeed, int energyCostRate) {
        this(type, acceleration, maxForwardSpeed, maxReverseSpeed, friction, steeringSpeed, energyCostRate, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, null, 1.0F);
    }
}
