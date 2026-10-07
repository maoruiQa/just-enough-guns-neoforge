package ttv.migami.jeg.client;

import java.util.ArrayList;

/** SW's single active impulse, with delayed arrivals tied to the current world. */
public final class ExplosionShakeState {
    public record Angles(float yaw, float pitch, float roll) {
        public static final Angles ZERO = new Angles(0, 0, 0);
    }
    private record Impulse(double x, double y, double z, double radius, double time,
                           double amplitude, double direction, long dueTick) {}
    private final ArrayList<Impulse> pending = new ArrayList<>();
    private Object world;
    private long ticks;
    private Impulse active;
    private double time;

    public void world(Object next) {
        if (world != next) {
            clear();
            world = next;
        }
    }

    public void clear() {
        pending.clear();
        active = null;
        time = 0;
        ticks = 0;
    }

    public boolean receive(double x, double y, double z, double radius, double duration,
                           double amplitude, double distance, double direction) {
        if (world == null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(radius) || radius <= 0 || !Double.isFinite(duration) || duration <= 0
                || !Double.isFinite(amplitude) || amplitude <= 0 || !Double.isFinite(distance)
                || distance < 0 || distance >= radius || !Double.isFinite(direction) || Math.abs(direction) > 1) return false;
        Impulse impulse = new Impulse(x, y, z, radius, duration, amplitude, direction,
                ticks + (long) Math.floor(distance / 17));
        if (impulse.dueTick <= ticks) activate(impulse);
        else pending.add(impulse);
        return true;
    }

    public long ticks() { return ticks; }

    public void tick() {
        ticks++;
        for (var iterator = pending.iterator(); iterator.hasNext();) {
            Impulse impulse = iterator.next();
            if (impulse.dueTick <= ticks) {
                activate(impulse);
                iterator.remove();
            }
        }
    }

    private void activate(Impulse impulse) {
        active = impulse;
        time = impulse.time;
    }

    public Angles sample(double deltaTicks, double x, double y, double z, int strength, boolean riding) {
        if (strength <= 0) { clear(); return Angles.ZERO; }
        if (active == null) return Angles.ZERO;
        if (!Double.isFinite(deltaTicks) || deltaTicks < 0 || !Double.isFinite(x)
                || !Double.isFinite(y) || !Double.isFinite(z)) return Angles.ZERO;
        time *= Math.max(0, 1 - 0.05 * deltaTicks);
        if (time < 0.001) { active = null; time = 0; return Angles.ZERO; }
        double distance = Math.sqrt(Math.pow(x - active.x, 2) + Math.pow(y - active.y, 2) + Math.pow(z - active.z, 2));
        double falloff = Math.max(0, Math.min(1, 1 - distance / active.radius));
        double wave = time * Math.sin(0.5 * Math.PI * time) * active.amplitude * Math.PI / 180
                * falloff * Math.min(strength, 100) / 100.0 * (riding ? 0.1 : 1);
        if (wave == 0) return Angles.ZERO;
        double yaw = wave * Math.abs(active.direction);
        double roll = wave * (active.direction > 0 ? -1 : 1);
        if (!Double.isFinite(wave) || Math.abs(wave) > Float.MAX_VALUE) { clear(); return Angles.ZERO; }
        return new Angles((float) yaw, (float) -yaw, (float) roll);
    }

    public static void main(String[] args) {
        ExplosionShakeState state = new ExplosionShakeState();
        Object world = new Object();
        state.world(world);
        assert state.receive(1000, 64, -1000, 64, 21, 52, 0, 0.8);
        Angles near = state.sample(0, 1000, 64, -1000, 100, false);
        assert near.yaw != 0 && near.pitch != 0 && near.roll != 0;
        Angles half = state.sample(0, 1032, 64, -1000, 100, false);
        assert Math.abs(half.roll / near.roll - 0.5) < 0.00001;
        assert state.sample(0, 1064, 64, -1000, 100, false).equals(Angles.ZERO);
        assert Math.abs(state.sample(0, 1000, 64, -1000, 25, false).roll / near.roll - 0.25) < 0.00001;
        assert Math.abs(state.sample(0, 1000, 64, -1000, 100, true).roll / near.roll - 0.1) < 0.00001;
        assert state.sample(0, 1000, 64, -1000, 100, false).equals(near) : "Paused sample must stay frozen";
        state.clear();
        assert state.receive(0, 0, 0, 64, 21, 52, 34, -0.8);
        state.tick(); assert state.active == null;
        state.tick(); assert state.active != null;
        state.receive(0, 0, 0, 64, 21, 52, 0, 0.8);
        assert state.active.direction == 0.8 : "Latest arrival replaces, rather than stacks";
        for (int i = 0; i < 300; i++) state.sample(1, 0, 0, 0, 100, false);
        assert state.active == null && state.sample(1, 0, 0, 0, 100, false).equals(Angles.ZERO);
        state.receive(0, 0, 0, 64, 21, 52, 0, 0.8);
        state.receive(0, 0, 0, 64, 21, 52, 34, 0.8);
        state.sample(0, 0, 0, 0, 0, false);
        assert state.pending.isEmpty() && state.active == null;
        state.receive(0, 0, 0, 64, 21, 52, 0, 0.8);
        state.receive(0, 0, 0, 64, 21, 52, 34, 0.8);
        state.world(new Object());
        assert state.pending.isEmpty() && state.active == null;
        assert !state.receive(Double.NaN, 0, 0, 64, 21, 52, 0, 0.8);
        assert !state.receive(0, 0, 0, 0, 21, 52, 0, 0.8);
        assert !state.receive(0, 0, 0, 64, Double.POSITIVE_INFINITY, 52, 0, 0.8);
        state.world(null);
        assert !state.receive(0, 0, 0, 64, 21, 52, 0, 0.8);
        System.out.println("Explosion shake: distance, delay, axes, pause, replacement, recovery, strength, vehicle and world reset passed");
    }
}
