package ttv.migami.jeg.client;

import net.minecraft.util.Mth;

/**
 * Maintains a small recoil curve that item rendering can query to add subtle kickback.
 */
public final class GunRecoilHandler {
    private static float recoil;
    private static float previous;
    private static float phase;
    private static float phaseSpeed;
    private static float amplitude;
    private static float pendingImpulse;
    private static boolean firedThisTick;

    // Frequency controls (radians/tick): speed has a hard cap for very high fire-rate weapons.
    private static final float BASE_PHASE_SPEED = 0.42F;
    private static final float MAX_PHASE_SPEED = 1.75F;
    private static final float SPEED_GAIN_PER_SHOT = 0.22F;

    // Amplitude controls.
    private static final float IMPULSE_SCALE = 0.42F;
    private static final float MIN_IMPULSE = 0.08F;
    private static final float MAX_IMPULSE = 0.45F;
    private static final float MAX_AMPLITUDE = 0.95F;
    private static final float AMPLITUDE_DAMPING_FIRING = 0.94F;
    private static final float AMPLITUDE_DAMPING_IDLE = 0.60F;

    private GunRecoilHandler() {}

    public static void tick() {
        previous = recoil;

        if (firedThisTick) {
            phaseSpeed = Mth.clamp(
                    Math.max(phaseSpeed, BASE_PHASE_SPEED) + SPEED_GAIN_PER_SHOT,
                    BASE_PHASE_SPEED,
                    MAX_PHASE_SPEED
            );
            amplitude = Mth.clamp(amplitude + pendingImpulse, 0.0F, MAX_AMPLITUDE);
        } else {
            phaseSpeed = Mth.approach(phaseSpeed, 0.0F, 0.22F);
            amplitude *= AMPLITUDE_DAMPING_IDLE;
        }

        if (amplitude > 0.0001F && phaseSpeed > 0.0001F) {
            phase += phaseSpeed;
            recoil = Mth.sin(phase) * amplitude;
            amplitude *= firedThisTick ? AMPLITUDE_DAMPING_FIRING : AMPLITUDE_DAMPING_IDLE;
        } else {
            recoil = 0.0F;
            if (!firedThisTick) {
                phase = 0.0F;
                amplitude = 0.0F;
                phaseSpeed = 0.0F;
            }
        }

        pendingImpulse = 0.0F;
        firedThisTick = false;
    }

    public static void addShot(float amount) {
        pendingImpulse = Mth.clamp(
                pendingImpulse + Mth.clamp(amount * IMPULSE_SCALE, MIN_IMPULSE, MAX_IMPULSE),
                0.0F,
                MAX_AMPLITUDE
        );
        firedThisTick = true;
    }

    public static void addDryFire(float amount) {
        pendingImpulse = Mth.clamp(
                pendingImpulse + Mth.clamp(amount * (IMPULSE_SCALE * 0.45F), MIN_IMPULSE * 0.5F, MAX_IMPULSE * 0.6F),
                0.0F,
                MAX_AMPLITUDE
        );
        firedThisTick = true;
    }

    public static void stopImmediate() {
        recoil = 0.0F;
        previous = 0.0F;
        phase = 0.0F;
        phaseSpeed = 0.0F;
        amplitude = 0.0F;
        pendingImpulse = 0.0F;
        firedThisTick = false;
    }

    public static float getRecoil(float partialTick) {
        return Mth.lerp(partialTick, previous, recoil);
    }

    public static float current() {
        return recoil;
    }
}
