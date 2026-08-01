package ttv.migami.jeg.item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import ttv.migami.jeg.gun.GunStats;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.projectile.VehicleMissileEntity;
import ttv.migami.jeg.vehicle.util.VehicleMissileProfile;

public final class GuidedLauncherItem extends AnimatedGunItem {
    private static final double RANGE = 256.0D;
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

    @Override
    public boolean tryShoot(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
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
        Vec3 direction = locked.position.subtract(shooter.getEyePosition()).normalize();
        Vec3 muzzle = shooter.getEyePosition().add(direction.scale(0.8D));
        boolean topAttack = !this.airOnly && launcherMode(stack) == 1;
        level.addFreshEntity(new VehicleMissileEntity(
                level, shooter, target, locked.position, muzzle, direction.scale(1.25D), this.getStats().id(), topAttack
        ));
    }

    @Nullable
    private LockTarget validateTarget(ServerPlayer player, int targetId) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        if (targetId < 0) {
            if (this.airOnly || !player.isShiftKeyDown()) {
                return null;
            }
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eye, eye.add(look.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
            ));
            return hit.getType() == HitResult.Type.BLOCK ? new LockTarget(-1, hit.getLocation()) : null;
        }

        Entity target = player.level().getEntity(targetId);
        if (target == null || !target.isAlive() || target == player || player.distanceToSqr(target) > RANGE * RANGE) {
            return null;
        }
        VehicleMissileProfile profile = VehicleMissileProfile.get(this.getStats().id());
        if (!profile.canLock(target, player, player.getVehicle() instanceof VehicleEntity vehicle ? vehicle : null)) {
            return null;
        }
        Vec3 aimPoint = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 toTarget = aimPoint.subtract(eye).normalize();
        if (look.dot(toTarget) < this.minimumDot || !player.hasLineOfSight(target)) {
            return null;
        }
        return new LockTarget(targetId, aimPoint);
    }

    private record LockState(LockTarget target, long startedTick, long lastTick) {}

    private record LockTarget(int entityId, Vec3 position) {
        private boolean sameAs(LockTarget other) {
            return this.entityId == other.entityId && (this.entityId >= 0 || this.position.distanceToSqr(other.position) < 4.0D);
        }
    }
}
