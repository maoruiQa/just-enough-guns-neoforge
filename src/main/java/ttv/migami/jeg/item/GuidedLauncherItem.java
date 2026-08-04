package ttv.migami.jeg.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;

public final class GuidedLauncherItem extends AnimatedGunItem {
    private static final double RANGE = 256.0D;
    /** SW javelin MaxTargetHeight */
    private static final double JAVELIN_MAX_TARGET_HEIGHT = 64.0D;
    /** SW igla MinTargetHeight */
    private static final double IGLA_MIN_TARGET_HEIGHT = 16.0D;
    private static final Map<UUID, LockState> LOCKS = new HashMap<>();
    private static final Map<UUID, LockTarget> PENDING_SHOTS = new HashMap<>();

    private final int lockTicks;
    private final double minimumDot;
    private final boolean airOnly;

    public GuidedLauncherItem(Properties properties, GunStats stats, int lockTicks, double lockAngle, boolean airOnly) {
        super(properties, stats);
        this.lockTicks = lockTicks;
        this.minimumDot = Math.cos(Math.toRadians(lockAngle));
        this.airOnly = airOnly;
    }

    public int lockTicks() {
        return this.lockTicks;
    }

    public double lockAngleDegrees() {
        return Math.toDegrees(Math.acos(this.minimumDot));
    }

    public boolean holdToFire() {
        return !this.airOnly;
    }

    public boolean airOnly() {
        return this.airOnly;
    }

    public static int launcherMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LAUNCHER_MODE.get(), 0);
    }

    public static void toggleMode(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof GuidedLauncherItem launcher) || launcher.airOnly) {
            return;
        }
        int mode = launcherMode(stack) == 0 ? 1 : 0;
        stack.set(ModDataComponents.LAUNCHER_MODE.get(), mode);
        player.displayClientMessage(Component.translatable(mode == 0 ? "message.jeg.javelin.direct" : "message.jeg.javelin.top"), true);
    }

    public static void updateLock(ServerPlayer player, InteractionHand hand, int targetId) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GuidedLauncherItem launcher) || !NetworkHandler.isAiming(player)) {
            LOCKS.remove(player.getUUID());
            return;
        }
        LockTarget target = launcher.validateTarget(player, targetId);
        if (target == null) {
            LOCKS.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        LockState previous = LOCKS.get(player.getUUID());
        long started = previous != null && previous.target.sameAs(target) && now - previous.lastTick <= 2L
                ? previous.startedTick
                : now;
        LOCKS.put(player.getUUID(), new LockState(target, started, now));
    }

    public static void clearLock(ServerPlayer player) {
        LOCKS.remove(player.getUUID());
        PENDING_SHOTS.remove(player.getUUID());
    }

    public static int clientLockProgress(Player player) {
        LockState lock = LOCKS.get(player.getUUID());
        if (lock == null || !(player.getMainHandItem().getItem() instanceof GuidedLauncherItem launcher)) {
            return 0;
        }
        long held = player.level().getGameTime() - lock.startedTick;
        return (int) Math.min(launcher.lockTicks(), Math.max(0L, held + 1L));
    }

    public static boolean isFullyLocked(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof GuidedLauncherItem launcher)) {
            return false;
        }
        LockState lock = LOCKS.get(player.getUUID());
        if (lock == null) {
            return false;
        }
        long now = player.level().getGameTime();
        return now - lock.lastTick <= 3L && now - lock.startedTick >= launcher.lockTicks();
    }

    @Override
    public boolean tryShoot(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!NetworkHandler.isAiming(serverPlayer)) {
                return false;
            }
            LockState lock = LOCKS.get(player.getUUID());
            long now = level.getGameTime();
            if (lock == null || now - lock.lastTick > 3L || now - lock.startedTick < this.lockTicks) {
                return false;
            }
            LockTarget current = this.validateTarget(serverPlayer, lock.target.entityId);
            if (current == null || !current.sameAs(lock.target)) {
                clearLock(serverPlayer);
                return false;
            }
            PENDING_SHOTS.put(player.getUUID(), current);
        }
        boolean fired = super.tryShoot(level, player, hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PENDING_SHOTS.remove(player.getUUID());
            if (fired) {
                LOCKS.remove(player.getUUID());
            }
        }
        return fired;
    }

    @Override
    public void fireAt(Level level, LivingEntity shooter, ItemStack stack, @Nullable LivingEntity ignored) {
        LockTarget locked = PENDING_SHOTS.get(shooter.getUUID());
        if (locked == null) {
            return;
        }
        Entity target = locked.entityId < 0 ? null : level.getEntity(locked.entityId);
        Vec3 look = shooter.getLookAngle();
        // SW muzzle offset style: slight forward/down from eye, initial velocity 3 with +0.3 Y bias
        Vec3 muzzle = shooter.getEyePosition().add(look.scale(0.15D)).add(0.0D, -0.2D, 0.0D);
        Vec3 velocity = new Vec3(look.x, look.y + 0.3D, look.z).normalize().scale(3.0D);
        boolean topAttack = !this.airOnly && launcherMode(stack) == 1;
        level.addFreshEntity(new VehicleMissileEntity(
                level, shooter, target, locked.position, muzzle, velocity, this.getStats().id(), topAttack
        ));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    shooter.getX() + 1.8D * look.x,
                    shooter.getY() + shooter.getBbHeight() - 0.1D + 1.8D * look.y,
                    shooter.getZ() + 1.8D * look.z,
                    30, 0.4D, 0.4D, 0.4D, 0.005D
            );
            playFireSounds(serverLevel, shooter, this.getStats().id());
        }
    }

    private static void playFireSounds(ServerLevel level, LivingEntity shooter, net.minecraft.resources.ResourceLocation weaponId) {
        String path = weaponId.getPath();
        // SW: 1P local volume 2, 3P distant volume 4, far volume 10
        if (shooter instanceof ServerPlayer player) {
            var oneP = ModSounds.ALL.get(Reference.id("item." + path + ".fire"));
            if (oneP != null) {
                level.playSound(player, shooter.blockPosition(), oneP.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
            }
        }
        playWorld(level, shooter, Reference.id("item." + path + ".fire_3p"), 4.0F);
        playWorld(level, shooter, Reference.id("item." + path + ".far"), 10.0F);
    }

    private static void playWorld(ServerLevel level, LivingEntity shooter, net.minecraft.resources.ResourceLocation id, float volume) {
        var holder = ModSounds.ALL.get(id);
        if (holder != null) {
            level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), holder.get(), SoundSource.PLAYERS, volume, 1.0F);
        }
    }

    @Nullable
    private LockTarget validateTarget(ServerPlayer player, int targetId) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        if (targetId < 0) {
            // Javelin ground-point lock (SW guideType 1) while sneaking
            if (this.airOnly || !player.isShiftKeyDown()) {
                return null;
            }
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return null;
            }
            Vec3 pos = hit.getLocation();
            if (Math.abs(pos.y - player.getY()) > JAVELIN_MAX_TARGET_HEIGHT) {
                return null;
            }
            return new LockTarget(-1, pos);
        }

        Entity target = player.level().getEntity(targetId);
        if (target == null || !target.isAlive() || target == player || player.distanceToSqr(target) > RANGE * RANGE) {
            return null;
        }
        if (ttv.migami.jeg.util.SmokeUtil.isSmokeBlockingLock(player, target)) {
            return null;
        }
        VehicleMissileProfile profile = VehicleMissileProfile.get(this.getStats().id());
        if (!profile.canLock(target, player, player.getVehicle() instanceof VehicleEntity vehicle ? vehicle : null)) {
            return null;
        }

        double heightDelta = target.getY() - player.getY();
        if (this.airOnly) {
            if (heightDelta < IGLA_MIN_TARGET_HEIGHT && !isExplicitAirTarget(target)) {
                // Allow true airborne entities even if relative height is low (e.g. flying near ground)
                if (target.onGround() || target.isInWater()) {
                    return null;
                }
            }
        } else if (heightDelta > JAVELIN_MAX_TARGET_HEIGHT) {
            return null;
        }

        Vec3 aimPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = aimPoint.subtract(eye).normalize();
        if (look.dot(toTarget) < this.minimumDot || !player.hasLineOfSight(target)) {
            return null;
        }
        return new LockTarget(targetId, aimPoint);
    }

    private static boolean isExplicitAirTarget(Entity target) {
        return target instanceof ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom
                || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || (target instanceof VehicleEntity vehicle
                && (vehicle.vehicleData().defaults().vehicleType() == ttv.migami.jeg.vehicle.data.subdata.VehicleType.HELICOPTER
                || vehicle.vehicleData().defaults().vehicleType() == ttv.migami.jeg.vehicle.data.subdata.VehicleType.AIRCRAFT));
    }

    private record LockState(LockTarget target, long startedTick, long lastTick) {}

    private record LockTarget(int entityId, Vec3 position) {
        private boolean sameAs(LockTarget other) {
            return this.entityId == other.entityId && (this.entityId >= 0 || this.position.distanceToSqr(other.position) < 4.0D);
        }
    }
}
