package ttv.migami.jeg.vehicle.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;

public final class EnemyVehicleController {
    public static final String ENEMY_VEHICLE_TAG = "JEGEnemyVehicle";
    public static final String ENEMY_VEHICLE_CREW_TAG = "JEGEnemyVehicleCrew";
    public static final String VEHICLE_KIND_TAG = "JEGEnemyVehicleKind";
    public static final String CREW_ID_TAG = "JEGEnemyVehicleCrewId";
    private static final String ANCHOR_TAG = "JEGEnemyVehicleAnchor";
    private static final String ANCHOR_X_TAG = "X";
    private static final String ANCHOR_Y_TAG = "Y";
    private static final String ANCHOR_Z_TAG = "Z";
    private static final String TARGET_ID_TAG = "JEGEnemyVehicleTarget";
    private static final String TARGET_MEMORY_TAG = "JEGEnemyVehicleTargetMemory";
    private static final int TARGET_MEMORY_TICKS = 100;
    private static final Map<UUID, Brain> BRAINS = new HashMap<>();

    private EnemyVehicleController() {}

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof VehicleEntity vehicle) || vehicle.level().isClientSide || !vehicle.getTags().contains(ENEMY_VEHICLE_TAG)) {
            return;
        }
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        Brain brain = BRAINS.computeIfAbsent(vehicle.getUUID(), ignored -> new Brain());
        tickVehicle(level, vehicle, brain);
    }

    public static void setAnchor(VehicleEntity vehicle, Vec3 anchor) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble(ANCHOR_X_TAG, anchor.x);
        tag.putDouble(ANCHOR_Y_TAG, anchor.y);
        tag.putDouble(ANCHOR_Z_TAG, anchor.z);
        vehicle.getPersistentData().put(ANCHOR_TAG, tag);
    }

    public static void configureCrew(LivingEntity crew) {
        crew.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        crew.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        crew.setInvisible(false);
        crew.noPhysics = true;
        if (crew instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
            mob.setCanPickUpLoot(false);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }

    private static void tickVehicle(ServerLevel level, VehicleEntity vehicle, Brain brain) {
        LivingEntity crew = crew(vehicle);
        if (crew == null) {
            if (vehicle.getPersistentData().hasUUID(CREW_ID_TAG)) {
                vehicle.destroyFromEnemyCrewLoss();
                BRAINS.remove(vehicle.getUUID());
                return;
            }
            stopVehicle(vehicle);
            return;
        }
        vehicle.getPersistentData().putUUID(CREW_ID_TAG, crew.getUUID());
        configureCrew(crew);

        String kind = vehicleKind(vehicle);
        Vec3 anchor = anchor(vehicle);
        Player target = updateTarget(level, vehicle, brain, kind);
        if (target == null) {
            patrol(vehicle, brain, anchor);
            vehicle.setAiWeaponControl(crew, false, false);
            return;
        }

        double distance = vehicle.distanceTo(target);
        boolean visible = canSee(vehicle, target);
        Aim aim = aimAt(vehicle, target);
        int weaponSlot = weaponSlot(kind, distance);
        vehicle.selectAiWeaponForSeat(0, weaponSlot);
        vehicle.setAiTurretAim(aim.turretYaw(), aim.pitch());

        double aimError = Math.max(Math.abs(Mth.wrapDegrees(vehicle.turretYaw() - aim.turretYaw())), Math.abs(vehicle.turretPitch() - aim.pitch()));
        boolean fire = visible && aimError <= aimTolerance(weaponSlot);
        vehicle.setAiWeaponControl(crew, fire, false);
        engage(vehicle, brain, target, kind, distance);
    }

    private static LivingEntity crew(VehicleEntity vehicle) {
        Entity passenger = vehicle.passengerForSeat(0);
        if (passenger instanceof LivingEntity living && living.isAlive() && living.getTags().contains(ENEMY_VEHICLE_CREW_TAG)) {
            return living;
        }
        return null;
    }

    private static String vehicleKind(VehicleEntity vehicle) {
        String saved = vehicle.getPersistentData().getString(VEHICLE_KIND_TAG);
        if (!saved.isBlank()) {
            var parsed = net.minecraft.resources.ResourceLocation.tryParse(saved);
            if (parsed != null) {
                return parsed.getPath();
            }
        }
        return vehicle.vehicleDataId().getPath();
    }

    private static Vec3 anchor(VehicleEntity vehicle) {
        CompoundTag tag = vehicle.getPersistentData().getCompound(ANCHOR_TAG);
        if (!tag.isEmpty()) {
            return new Vec3(tag.getDouble(ANCHOR_X_TAG), tag.getDouble(ANCHOR_Y_TAG), tag.getDouble(ANCHOR_Z_TAG));
        }
        Vec3 anchor = vehicle.position();
        setAnchor(vehicle, anchor);
        return anchor;
    }

    private static Player updateTarget(ServerLevel level, VehicleEntity vehicle, Brain brain, String kind) {
        if (brain.targetId == null && vehicle.getPersistentData().hasUUID(TARGET_ID_TAG)) {
            brain.targetId = vehicle.getPersistentData().getUUID(TARGET_ID_TAG);
            brain.targetMemory = vehicle.getPersistentData().getInt(TARGET_MEMORY_TAG);
        }
        double range = "bmp2".equals(kind) ? 76.0D : 72.0D;
        Player nearest = null;
        double nearestDistance = range * range;
        for (Player player : level.players()) {
            if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
                continue;
            }
            double distance = player.distanceToSqr(vehicle);
            if (distance <= nearestDistance && canSee(vehicle, player)) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            brain.targetId = nearest.getUUID();
            brain.targetMemory = TARGET_MEMORY_TICKS;
            vehicle.getPersistentData().putUUID(TARGET_ID_TAG, brain.targetId);
            vehicle.getPersistentData().putInt(TARGET_MEMORY_TAG, brain.targetMemory);
            return nearest;
        }
        if (brain.targetId != null && brain.targetMemory-- > 0) {
            Entity remembered = level.getEntity(brain.targetId);
            if (remembered instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.distanceTo(vehicle) <= range + 16.0D) {
                vehicle.getPersistentData().putInt(TARGET_MEMORY_TAG, brain.targetMemory);
                return player;
            }
        }
        brain.targetId = null;
        brain.targetMemory = 0;
        vehicle.getPersistentData().remove(TARGET_ID_TAG);
        vehicle.getPersistentData().remove(TARGET_MEMORY_TAG);
        return null;
    }

    private static void patrol(VehicleEntity vehicle, Brain brain, Vec3 anchor) {
        if (brain.patrolTarget == null || vehicle.position().distanceToSqr(brain.patrolTarget) < 16.0D || vehicle.tickCount % 160 == 0) {
            brain.patrolTarget = nextDryPatrolTarget(vehicle, anchor);
        }
        driveToward(vehicle, brain, brain.patrolTarget, 12.0D, 8.0D, false);
    }

    private static Vec3 nextDryPatrolTarget(VehicleEntity vehicle, Vec3 anchor) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = (vehicle.getRandom().nextDouble() * Math.PI * 2.0D);
            double radius = 24.0D + vehicle.getRandom().nextDouble() * 24.0D;
            Vec3 candidate = anchor.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            if (!isWaterAt(vehicle, candidate)) {
                return candidate;
            }
        }
        return anchor;
    }

    private static void engage(VehicleEntity vehicle, Brain brain, Player target, String kind, double distance) {
        double approach = "bmp2".equals(kind) ? 52.0D : 42.0D;
        double retreat = "bmp2".equals(kind) ? 22.0D : 18.0D;
        double hold = "bmp2".equals(kind) ? 30.0D : 24.0D;
        if (distance > approach) {
            driveToward(vehicle, brain, target.position(), 7.0D, 5.0D, false);
        } else if (distance < retreat) {
            driveToward(vehicle, brain, target.position(), 180.0D, 5.0D, true);
        } else {
            boolean side = ((vehicle.tickCount / 80) & 1) == 0;
            Vec3 offset = target.position().subtract(vehicle.position()).normalize();
            Vec3 strafe = new Vec3(offset.z, 0.0D, -offset.x).scale(side ? hold : -hold);
            driveToward(vehicle, brain, target.position().add(strafe), 18.0D, 7.0D, false);
        }
    }

    private static void driveToward(VehicleEntity vehicle, Brain brain, Vec3 target, double stopDistance, double yawDeadZone, boolean reverse) {
        updateStuckState(vehicle, brain);
        if (brain.reverseTicks > 0) {
            brain.reverseTicks--;
            vehicle.setAiVehicleInput(input(false, true, true, false, false));
            return;
        }
        Vec3 toTarget = target.subtract(vehicle.position());
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (horizontalDistance < stopDistance) {
            vehicle.setAiVehicleInput(input(false, false, false, false, true));
            return;
        }
        float desiredYaw = (float) -Math.toDegrees(Math.atan2(toTarget.x, toTarget.z));
        float yawDiff = Mth.wrapDegrees(desiredYaw - vehicle.getYRot());
        boolean left = yawDiff < -yawDeadZone;
        boolean right = yawDiff > yawDeadZone;
        if (!reverse && Math.abs(yawDiff) < 35.0F && isForwardUnsafe(vehicle)) {
            brain.reverseTicks = 24;
            vehicle.setAiVehicleInput(input(false, true, true, false, false));
            return;
        }
        vehicle.setAiVehicleInput(input(!reverse, reverse, left, right, false));
    }

    private static void updateStuckState(VehicleEntity vehicle, Brain brain) {
        Vec3 current = vehicle.position();
        if (brain.lastPosition != null && vehicle.tickCount % 20 == 0) {
            if (current.distanceToSqr(brain.lastPosition) < 0.25D) {
                brain.stuckTicks += 20;
                if (brain.stuckTicks >= 40) {
                    brain.reverseTicks = 30;
                    brain.stuckTicks = 0;
                }
            } else {
                brain.stuckTicks = 0;
            }
            brain.lastPosition = current;
        } else if (brain.lastPosition == null) {
            brain.lastPosition = current;
        }
    }

    private static boolean isForwardUnsafe(VehicleEntity vehicle) {
        Vec3 start = vehicle.position().add(0.0D, 1.0D, 0.0D);
        double yaw = Math.toRadians(vehicle.getYRot());
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 end = start.add(forward.scale(4.0D));
        HitResult hit = vehicle.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        if (hit.getType() != HitResult.Type.MISS) {
            return true;
        }
        Vec3 low = vehicle.position().add(0.0D, 0.1D, 0.0D);
        for (int step = 3; step <= 8; step++) {
            Vec3 sample = low.add(forward.scale(step));
            if (isWaterAt(vehicle, sample)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWaterAt(VehicleEntity vehicle, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);
        return vehicle.level().getFluidState(pos).is(FluidTags.WATER)
                || vehicle.level().getFluidState(pos.below()).is(FluidTags.WATER);
    }

    private static VehicleInput input(boolean forward, boolean backward, boolean left, boolean right, boolean brake) {
        return new VehicleInput(forward, backward, left, right, brake, false, false, false, false, false, false, false, -1, false, false, 0.0F, 0.0F);
    }

    private static Aim aimAt(VehicleEntity vehicle, LivingEntity target) {
        Vec3 muzzle = vehicle.position().add(0.0D, 2.4D, 0.0D);
        Vec3 targetPos = target.getEyePosition();
        Vec3 delta = targetPos.subtract(muzzle);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float worldYaw = (float) -Math.toDegrees(Math.atan2(delta.x, delta.z));
        float turretYaw = Mth.wrapDegrees(vehicle.getYRot() - worldYaw);
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -15.0F, 32.5F);
        return new Aim(turretYaw, pitch);
    }

    private static int weaponSlot(String kind, double distance) {
        if ("bmp2".equals(kind)) {
            return distance < 28.0D ? 1 : 0;
        }
        return distance < 24.0D ? 1 : 0;
    }

    private static double aimTolerance(int weaponSlot) {
        return weaponSlot == 1 ? 8.0D : 5.0D;
    }

    private static boolean canSee(VehicleEntity vehicle, LivingEntity target) {
        Vec3 start = vehicle.position().add(0.0D, 2.4D, 0.0D);
        Vec3 end = target.getEyePosition();
        HitResult hit = vehicle.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void stopVehicle(VehicleEntity vehicle) {
        vehicle.setAiVehicleInput(VehicleInput.EMPTY);
        vehicle.setAiWeaponControl(null, false, false);
    }

    private record Aim(float turretYaw, float pitch) {}

    private static final class Brain {
        private UUID targetId;
        private int targetMemory;
        private Vec3 patrolTarget;
        private Vec3 lastPosition;
        private int stuckTicks;
        private int reverseTicks;
    }
}
