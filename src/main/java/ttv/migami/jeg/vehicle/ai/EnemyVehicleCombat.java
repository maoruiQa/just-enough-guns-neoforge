package ttv.migami.jeg.vehicle.ai;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.raid.RaidDifficulty;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

/** Server-authoritative attack rounds, shared by all seats of an enemy vehicle. */
public final class EnemyVehicleCombat {
    public static final String PROJECTILE_TAG = "JEGAdaptiveEnemyVehicleProjectile";
    private static final Map<Player, DamageWindow> WINDOWS = new WeakHashMap<>();

    private EnemyVehicleCombat() {}

    public record Settings(int windup, int burst, int pause, int interval, int maxShots, double spread) {
        public static Settings forTarget(double pressure, boolean explosive) {
            return forTarget(pressure, explosive, false);
        }

        public static Settings forTarget(double pressure, boolean explosive, boolean cannon) {
            double q = Double.isFinite(pressure) ? Mth.clamp(pressure, 0, 1) : 0;
            // A spawned vehicle always has a combat baseline; equipment mainly tightens its aim.
            double tempo = Math.max(.4, q);
            if (explosive) return new Settings((int) Math.round(80 - 30 * tempo), 20,
                    (int) Math.round(200 - 80 * tempo), 20, 1, cannon ? 5 - 2 * q : 2.2 - 1.2 * q);
            int interval = (int) Math.round(3 - q), shots = 10 + (int) (6 * q);
            // Include controller latency so the whole suppressive burst can fire.
            return new Settings(0, interval * (shots - 1) + 2,
                    (int) Math.round(20 - 8 * q), interval, shots, 2 - q);
        }
    }

    public static final class Cycle {
        private UUID targetId;
        private UUID mountId;
        private Settings settings;
        private Vec3 aimPoint;
        private long startedAt;
        private long nextRoundAt;
        private long nextHeavyAt;
        private boolean explosive;
        private int machineGunPause;
        private long lastShotAt = Long.MIN_VALUE / 2;
        private long firstShotAt;
        private int shots;
        private int rounds;
        private int seat;
        private int slot;

        public void cancel() {
            if (settings != null && shots > 0) nextRoundAt = Math.max(nextRoundAt, lastShotAt + (explosive ? machineGunPause : settings.pause));
            settings = null;
            aimPoint = null;
        }

        private boolean canFire(long now) {
            return settings != null && now >= startedAt + settings.windup
                    && now <= firingEnd()
                    && shots < settings.maxShots && now - lastShotAt >= settings.interval;
        }

        private long firingEnd() {
            // AI controls are assigned after the vehicle tick; measure the burst from the actual first shot.
            return (shots > 0 ? firstShotAt : startedAt + settings.windup + 1) + settings.burst;
        }
    }

    public static double mountStrength(Player player) {
        if (!(player.getRootVehicle() instanceof VehicleEntity vehicle)) return 0;
        return switch (vehicle.vehicleDataId().getPath()) {
            case "lav150" -> .7;
            case "bmp2" -> .85;
            case "ah6" -> .8;
            case "mi28" -> 1;
            default -> .4;
        };
    }

    public static boolean combatMounted(Player player) {
        return mountStrength(player) >= .7;
    }

    public static void control(VehicleEntity vehicle, LivingEntity[] crew, Player target, boolean visible, double distance, Cycle cycle) {
        vehicle.setAiWeaponControl(null, false, false);
        long now = vehicle.level().getGameTime();
        String kind = vehicle.vehicleDataId().getPath();
        double range = kind.equals("mi28") ? 150 : kind.equals("ah6") ? 90 : 100;
        UUID mount = target.getRootVehicle() == target ? null : target.getRootVehicle().getUUID();
        if (!visible || distance > range || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            cycle.cancel();
            return;
        }
        if (cycle.settings != null && (!target.getUUID().equals(cycle.targetId)
                || !java.util.Objects.equals(mount, cycle.mountId))) cycle.cancel();
        if (cycle.settings != null && (cycle.shots >= cycle.settings.maxShots
                || now > cycle.firingEnd())) {
            if (cycle.explosive) cycle.nextHeavyAt = Math.max(cycle.nextHeavyAt, now + cycle.settings.pause);
            cycle.nextRoundAt = now + (cycle.explosive ? cycle.machineGunPause : cycle.settings.pause);
            cycle.rounds++;
            cycle.cancel();
        }
        if (cycle.settings == null) {
            if (now < cycle.nextRoundAt) return;
            boolean explosive = cycle.rounds >= 2 && now >= cycle.nextHeavyAt && distance >= (kind.equals("mi28") || kind.equals("ah6") ? 40 : 24);
            boolean mounted = combatMounted(target);
            cycle.seat = 0;
            cycle.slot = 0;
            if (kind.equals("mi28")) {
                cycle.seat = explosive ? 0 : 1;
                cycle.slot = explosive ? (mounted ? (target.getRootVehicle() instanceof VehicleEntity v
                        && (v.vehicleDataId().getPath().equals("ah6") || v.vehicleDataId().getPath().equals("mi28")) ? 2 : 1) : 0) : 3;
            } else if (kind.equals("ah6")) {
                cycle.slot = explosive ? 1 : 0;
            } else if (kind.equals("lav150") || kind.equals("bmp2")) {
                boolean airborne = target.getRootVehicle() instanceof VehicleEntity v
                        && (v.vehicleData().defaults().vehicleType() == ttv.migami.jeg.vehicle.data.subdata.VehicleType.HELICOPTER
                        || v.vehicleData().defaults().vehicleType() == ttv.migami.jeg.vehicle.data.subdata.VehicleType.AIRCRAFT);
                cycle.slot = explosive ? (kind.equals("bmp2") && mounted && !airborne ? 2 : 0) : 1;
            } else {
                explosive = false;
            }
            if (cycle.seat >= crew.length || cycle.slot >= vehicle.vehicleData().defaults().weapons().size()) return;
            double q = Math.max(RaidDifficulty.equipmentScore(target) / 100, mountStrength(target));
            String weapon = vehicle.vehicleData().defaults().weapons().get(cycle.slot).weaponId().getPath();
            boolean cannon = weapon.equals("vehicle_20mm_cannon") || weapon.equals("vehicle_30mm_cannon");
            cycle.settings = Settings.forTarget(q, explosive, cannon);
            cycle.explosive = explosive;
            cycle.machineGunPause = Settings.forTarget(q, false).pause;
            cycle.targetId = target.getUUID();
            cycle.mountId = mount;
            cycle.startedAt = now;
            cycle.shots = 0;
            cycle.lastShotAt = Long.MIN_VALUE / 2;
            cycle.aimPoint = aimPoint(target);
            if (explosive) VehicleEntity.warnEnemyAttack(target);
        }
        if (!cycle.explosive || now < cycle.startedAt + cycle.settings.windup - 8) cycle.aimPoint = aimPoint(target);
        LivingEntity shooter = crew[cycle.seat];
        vehicle.selectAiWeaponForSeat(cycle.seat, cycle.slot);
        Vec3 muzzle = vehicle.aiWeaponMuzzlePosition(cycle.slot);
        Vec3 delta = cycle.aimPoint.subtract(muzzle);
        float yaw = (float) -Math.toDegrees(Math.atan2(delta.x, delta.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
        var turret = vehicle.vehicleData().defaults().turret();
        vehicle.setAiTurretAim(Mth.wrapDegrees(vehicle.getYRot() - yaw), Mth.clamp(pitch, -turret.maxPitch(), -turret.minPitch()));
        shooter.setYRot(yaw);
        shooter.setYHeadRot(yaw);
        shooter.yBodyRot = yaw;
        shooter.setXRot(pitch);
        boolean aligned = Math.abs(Mth.wrapDegrees(yaw - vehicle.getYRot())) < (cycle.seat == 0
                && (kind.equals("ah6") || kind.equals("mi28")) ? 12 : 110);
        boolean guided = vehicle.vehicleData().defaults().weapons().get(cycle.slot).guided();
        vehicle.setAiWeaponControlForSeat(cycle.seat, shooter, aligned && cycle.canFire(now + 1), guided);
    }

    private static Vec3 aimPoint(Player target) {
        return (combatMounted(target) ? target.getRootVehicle() : target).getBoundingBox().getCenter();
    }

    public static boolean applies(VehicleEntity vehicle, LivingEntity shooter) {
        return Config.enemyVehicleAdaptiveCombatEnabled() && vehicle.isEnemyAiVehicle()
                && shooter != null && shooter.getTags().contains(EnemyVehicleController.ENEMY_VEHICLE_CREW_TAG);
    }

    public static Vec3 scatter(Vec3 direction, double degrees, double radiusRoll, double angleRoll) {
        Vec3 forward = direction.normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        if (right.lengthSqr() < .0001) right = new Vec3(1, 0, 0);
        Vec3 up = right.cross(forward).normalize();
        double radius = Math.tan(Math.toRadians(degrees)) * Math.sqrt(radiusRoll);
        double angle = angleRoll * Math.PI * 2;
        return forward.add(right.scale(radius * Math.cos(angle))).add(up.scale(radius * Math.sin(angle))).normalize();
    }

    public static Vec3 shotDirection(VehicleEntity vehicle, LivingEntity shooter, Vec3 original, Cycle cycle) {
        if (!applies(vehicle, shooter) || cycle.settings == null || cycle.aimPoint == null) return original;
        Vec3 direction = cycle.aimPoint.subtract(vehicle.aiWeaponMuzzlePosition(cycle.slot)).normalize();
        return scatter(direction, cycle.settings.spread, vehicle.getRandom().nextDouble(), vehicle.getRandom().nextDouble());
    }

    public static boolean allowShot(VehicleEntity vehicle, LivingEntity shooter, Cycle cycle) {
        return !applies(vehicle, shooter) || cycle.canFire(vehicle.level().getGameTime())
                && vehicle.getSeatIndex(shooter) == cycle.seat;
    }

    public static int fireDelay(VehicleEntity vehicle, LivingEntity shooter, int original, Cycle cycle) {
        if (!applies(vehicle, shooter) || cycle.settings == null) return original;
        // Adaptive AI machine guns use their suppression cadence; player weapons retain their own stats.
        return cycle.explosive ? Math.max(original, cycle.settings.interval) : cycle.settings.interval;
    }

    public static void fired(VehicleEntity vehicle, LivingEntity shooter, Cycle cycle) {
        if (applies(vehicle, shooter) && cycle.settings != null) {
            if (cycle.shots == 0) cycle.firstShotAt = vehicle.level().getGameTime();
            if (cycle.explosive) cycle.nextHeavyAt = vehicle.level().getGameTime() + cycle.settings.pause;
            cycle.shots++;
            cycle.lastShotAt = vehicle.level().getGameTime();
        }
    }

    public static void markProjectile(Entity projectile, Entity owner) {
        if (owner instanceof LivingEntity shooter && shooter.getVehicle() instanceof VehicleEntity vehicle
                && applies(vehicle, shooter)) projectile.addTag(PROJECTILE_TAG);
    }

    private static boolean protectedTarget(Entity target, DamageSource source) {
        if (!(target instanceof Player player) || player.isPassenger() || source == null) return false;
        Entity direct = source.getDirectEntity();
        return direct != null && direct.getTags().contains(PROJECTILE_TAG)
                || Config.enemyVehicleAdaptiveCombatEnabled() && direct instanceof VehicleEntity vehicle && vehicle.isEnemyAiVehicle();
    }

    public static float limitDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(amount > 0) || target.level().isClientSide || !protectedTarget(target, source)) return amount;
        Player player = (Player) target;
        return WINDOWS.computeIfAbsent(player, ignored -> new DamageWindow()).takeDamage(player.level().getGameTime(), player.getMaxHealth() * .4F, amount);
    }

    public static Vec3 limitImpulse(Entity target, DamageSource source, Vec3 impulse) {
        if (!protectedTarget(target, source)) return impulse;
        Player player = (Player) target;
        return WINDOWS.computeIfAbsent(player, ignored -> new DamageWindow()).takeImpulse(player.level().getGameTime(), impulse);
    }

    private record DamageUse(long tick, float amount) {}
    private record ImpulseUse(long tick, double horizontal, double upward) {}

    public static final class DamageWindow {
        private final ArrayDeque<DamageUse> damage = new ArrayDeque<>();
        private final ArrayDeque<ImpulseUse> impulses = new ArrayDeque<>();

        public float takeDamage(long now, float budget, float amount) {
            while (!damage.isEmpty() && now - damage.peekFirst().tick >= 20) damage.removeFirst();
            float used = 0;
            for (DamageUse hit : damage) used += hit.amount;
            float result = Math.max(0, Math.min(amount, budget - used));
            if (result > 0) damage.addLast(new DamageUse(now, result));
            return result;
        }

        public Vec3 takeImpulse(long now, Vec3 impulse) {
            while (!impulses.isEmpty() && now - impulses.peekFirst().tick >= 20) impulses.removeFirst();
            double horizontalUsed = 0, upwardUsed = 0;
            for (ImpulseUse hit : impulses) { horizontalUsed += hit.horizontal; upwardUsed += hit.upward; }
            double horizontal = Math.min(impulse.horizontalDistance(), Math.max(0, .35 - horizontalUsed));
            double upward = Math.min(Math.max(0, impulse.y), Math.max(0, .18 - upwardUsed));
            double scale = impulse.horizontalDistance() > 0 ? horizontal / impulse.horizontalDistance() : 0;
            if (horizontal > 0 || upward > 0) impulses.addLast(new ImpulseUse(now, horizontal, upward));
            return new Vec3(impulse.x * scale, impulse.y > 0 ? upward : impulse.y, impulse.z * scale);
        }
    }

    public static void selfCheck() {
        Settings weak = Settings.forTarget(0, false), strong = Settings.forTarget(1, false);
        assert weak.windup == 0 && weak.burst == 29 && weak.pause == 20 && weak.maxShots == 10 && weak.interval == 3;
        assert strong.windup == 0 && strong.burst == 32 && strong.pause == 12 && strong.maxShots == 16 && strong.interval == 2;
        assert weak.spread == 2 && strong.spread == 1;
        assert Settings.forTarget(0, true, true).spread == 5 && Settings.forTarget(1, true, true).spread == 3;
        assert Settings.forTarget(0, true).spread == 2.2 && Math.abs(Settings.forTarget(1, true).spread - 1) < .00001;
        assert Settings.forTarget(1, true).windup == 50 && Settings.forTarget(1, true).pause == 120 && Settings.forTarget(1, true).maxShots == 1;
        assert Settings.forTarget(0, true).windup == 68 && Settings.forTarget(0, true).pause == 168;
        Cycle cycle = new Cycle();
        cycle.settings = weak;
        assert cycle.canFire(0) && cycle.canFire(30) && !cycle.canFire(31);
        cycle.firstShotAt = cycle.lastShotAt = 1;
        cycle.shots = 1;
        assert !cycle.canFire(3) && cycle.canFire(4);
        cycle.shots = 10;
        assert !cycle.canFire(30);
        cycle.cancel();
        assert !cycle.canFire(100) && cycle.nextRoundAt == 21;
        // Heavy cooldown never suppresses the following machine-gun round.
        cycle.settings = Settings.forTarget(0, true);cycle.explosive = true;cycle.machineGunPause = 20;cycle.shots = 1;
        cycle.cancel();assert cycle.nextRoundAt == 21;
        DamageWindow window = new DamageWindow();
        assert window.takeDamage(0, 8, 650) == 8;
        assert window.takeDamage(1, 8, 80) == 0;
        assert window.takeDamage(19, 8, 10) == 0;
        assert window.takeDamage(20, 8, 3) == 3;
        assert window.takeDamage(21, 8, 100) == 5;
        Vec3 impulse = window.takeImpulse(0, new Vec3(3, 4, 0));
        assert Math.abs(impulse.x - .35) < .00001 && Math.abs(impulse.y - .18) < .00001;
        assert window.takeImpulse(1, new Vec3(1, 1, 0)).lengthSqr() == 0;
        assert window.takeImpulse(20, new Vec3(1, 1, 0)).lengthSqr() > 0;
        Vec3 scattered = scatter(new Vec3(0, 0, 1), 8, 1, .25);
        assert Math.abs(scattered.length() - 1) < .00001 && scattered.distanceTo(new Vec3(0, 0, 1)) > .1;
    }
}
