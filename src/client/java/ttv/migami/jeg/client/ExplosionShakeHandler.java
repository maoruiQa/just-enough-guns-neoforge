package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.network.ExplosionShakePayload;

public final class ExplosionShakeHandler {
    private static final ExplosionShakeState STATE = new ExplosionShakeState();

    private ExplosionShakeHandler() {}

    private static boolean ready(Minecraft mc) {
        STATE.world(mc.level);
        if (mc.level == null || mc.player == null || mc.player.isSpectator() || Config.explosionScreenShake() <= 0) {
            STATE.clear();
            return false;
        }
        return true;
    }

    public static void receive(ExplosionShakePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (!payload.valid()) return;
        ExplosionShakeCheck.notification();
        if (!ready(mc)) return;
        double distance = mc.player.position().distanceTo(new Vec3(payload.x(), payload.y(), payload.z()));
        if (STATE.receive(payload.x(), payload.y(), payload.z(), payload.radius(), payload.time(),
                payload.amplitude(), distance, 2 * Math.random() - 1)) {
            ExplosionShakeCheck.arrival(STATE.ticks() + (long) Math.floor(distance / 17));
        }
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (ready(mc) && !mc.isPaused()) STATE.tick();
        ExplosionShakeCheck.tick();
    }

    static void reset() { STATE.clear(); }

    static long ticks() { return STATE.ticks(); }

    public static ExplosionShakeState.Angles frame() {
        Minecraft mc = Minecraft.getInstance();
        if (!ready(mc)) return ExplosionShakeState.Angles.ZERO;
        Vec3 position = mc.player.position();
        double delta = mc.isPaused() ? 0 : mc.getDeltaTracker().getGameTimeDeltaTicks();
        return STATE.sample(delta, position.x, position.y, position.z,
                Config.explosionScreenShake(), mc.player.getVehicle() != null);
    }
}
