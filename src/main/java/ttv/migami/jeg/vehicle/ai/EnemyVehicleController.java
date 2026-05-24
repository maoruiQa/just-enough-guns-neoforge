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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;
import ttv.migami.jeg.vehicle.entity.base.VehicleInput;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;

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

    public static void tickEntity(Entity entity) {
        if (!(entity instanceof VehicleEntity vehicle) || vehicle.level().isClientSide || !vehicle.getTags().contains(ENEMY_VEHICLE_TAG)) {
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
        String kind = vehicleKind(vehicle);
        LivingEntity[] crews = crews(vehicle, requiredCrew(kind));
        if (crews[0] == null || missingRequiredCrew(crews)) {
            if (vehicle.getPersistentData().hasUUID(CREW_ID_TAG)) {
                vehicle.destroyFromEnemyCrewLoss();
                BRAINS.remove(vehicle.getUUID());
                return;
            }
            stopVehicle(vehicle);
            return;
        }
        vehicle.getPersistentData().putUUID(CREW_ID_TAG, crews[0].getUUID());
        for (LivingEntity crew : crews) {
            configureCrew(crew);
        }

        if (isAirVehicle(kind)) {
            tickAirVehicle(level, vehicle, brain, kind, crews);
            return;
        }

        Vec3 anchor = anchor(vehicle);
        Player target = updateTarget(level, vehicle, brain, kind);
        if (target == null) {
            patrol(vehicle, brain, anchor);
            vehicle.setAiWeaponControl(crews[0], false, false);
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
        aimCrewAt(crews[0], aim);
        vehicle.setAiWeaponControl(crews[0], fire, false);
        engage(vehicle, brain, target, kind, distance);
    }

    private static LivingEntity[] crews(VehicleEntity vehicle, int count) {
        LivingEntity[] crews = new LivingEntity[count];
        for (int seat = 0; seat < count; seat++) {
            crews[seat] = crew(vehicle, seat);
        }
        return crews;
    }

    private static LivingEntity crew(VehicleEntity vehicle, int seatIndex) {
        Entity passenger = vehicle.passengerForSeat(seatIndex);
        if (passenger instanceof LivingEntity living && living.isAlive() && living.getTags().contains(ENEMY_VEHICLE_CREW_TAG)) {
            return living;
        }
        return null;
    }

    private static boolean missingRequiredCrew(LivingEntity[] crews) {
        for (LivingEntity crew : crews) {
            if (crew == null) {
                return true;
            }
        }
        return false;
    }

    private static int requiredCrew(String kind) {
        return "mi28".equals(kind) ? 2 : 1;
    }

    private static boolean isAirVehicle(String kind) {
        return "ah6".equals(kind) || "mi28".equals(kind);
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
        double range = detectionRange(kind);
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
            if (remembered instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator() && player.distanceTo(vehicle) <= targetRetentionRange(kind)) {
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

    private static double detectionRange(String kind) {
        return switch (kind) {
            case "mi28" -> 140.0D;
            case "ah6" -> 90.0D;
            case "bmp2" -> 76.0D;
            default -> 72.0D;
        };
    }

    private static double targetRetentionRange(String kind) {
        return switch (kind) {
            case "mi28" -> 220.0D;
            case "ah6" -> 150.0D;
            default -> detectionRange(kind) + 16.0D;
        };
    }

    private static void tickAirVehicle(ServerLevel level, VehicleEntity vehicle, Brain brain, String kind, LivingEntity[] crews) {
        Vec3 anchor = anchor(vehicle);
        Player target = updateTarget(level, vehicle, brain, kind);
        if (target == null) {
            airPatrol(vehicle, brain, anchor, kind);
            vehicle.setAiWeaponControlForSeat(0, crews[0], false, false);
            if (crews.length > 1) {
                vehicle.setAiWeaponControlForSeat(1, crews[1], false, false);
            }
            vehicle.setAiWeaponControl(null, false, false);
            return;
        }

        double distance = vehicle.distanceTo(target);
        Aim aim = airAimAt(vehicle, target.getEyePosition(), kind);
        boolean visible = canSee(vehicle, target);
        aimCrewAt(crews[0], aim);
        if ("mi28".equals(kind)) {
            tickMi28Weapons(vehicle, crews, target, aim, distance, visible);
        } else {
            tickAh6Weapons(vehicle, crews[0], target, aim, distance, visible);
        }
        airEngage(vehicle, brain, target, kind, distance);
    }

    private static void tickAh6Weapons(VehicleEntity vehicle, LivingEntity pilot, Player target, Aim aim, double distance, boolean visible) {
        double noseError = Math.abs(Mth.wrapDegrees(aim.worldYaw() - vehicle.getYRot()));
        boolean stable = Math.abs(vehicle.roll()) < 35.0F && Math.abs(vehicle.getXRot()) < 28.0F;
        boolean useRocket = distance >= 42.0D && distance <= 82.0D && noseError < 7.0D && stable;
        int slot = useRocket ? 1 : 0;
        vehicle.selectAiWeaponForSeat(0, slot);
        boolean fire = visible && (slot == 0 ? noseError < 12.0D && distance <= 70.0D : useRocket);
        vehicle.setAiWeaponControlForSeat(0, pilot, fire, false);
    }

    private static void tickMi28Weapons(VehicleEntity vehicle, LivingEntity[] crews, Player target, Aim aim, double distance, boolean visible) {
        LivingEntity pilot = crews[0];
        LivingEntity gunner = crews[1];
        double noseError = Math.abs(Mth.wrapDegrees(aim.worldYaw() - vehicle.getYRot()));
        boolean stable = Math.abs(vehicle.roll()) < 30.0F && Math.abs(vehicle.getXRot()) < 24.0F;
        TargetVehicleClass targetVehicle = targetVehicleClass(target);

        int pilotSlot = switch (targetVehicle) {
            case SURFACE -> 1;
            case AIR -> 2;
            case NONE -> 0;
        };
        vehicle.selectAiWeaponForSeat(0, pilotSlot);
        boolean pilotMissile = pilotSlot == 1 || pilotSlot == 2;
        boolean pilotFire = visible && stable && noseError < (pilotMissile ? 5.0D : 7.0D) && distance >= 55.0D && distance <= 150.0D;
        vehicle.setAiWeaponControlForSeat(0, pilot, pilotFire, pilotMissile);

        int gunnerSlot = targetVehicle == TargetVehicleClass.SURFACE && distance >= 85.0D ? 4 : 3;
        vehicle.selectAiWeaponForSeat(1, gunnerSlot);
        Aim gunnerAim = gunnerSlot == 3
                ? airAimAt(vehicle, mi28GunnerSweepTarget(vehicle, target), "mi28", vehicle.aiWeaponMuzzlePosition(gunnerSlot))
                : airAimAt(vehicle, target.getEyePosition(), "mi28", vehicle.aiWeaponMuzzlePosition(gunnerSlot));
        vehicle.setAiTurretAim(gunnerAim.turretYaw(), Mth.clamp(gunnerAim.pitch(), -10.0F, 40.0F));
        aimCrewAt(gunner, gunnerAim);
        double turretError = Math.max(Math.abs(Mth.wrapDegrees(vehicle.turretYaw() - gunnerAim.turretYaw())), Math.abs(vehicle.turretPitch() - gunnerAim.pitch()));
        boolean gunnerFire = visible && (gunnerSlot == 3 ? turretError <= 8.0D && distance <= 110.0D : turretError <= 5.0D);
        vehicle.setAiWeaponControlForSeat(1, gunner, gunnerFire, gunnerSlot == 4);
    }

    private static Vec3 mi28GunnerSweepTarget(VehicleEntity vehicle, Player target) {
        Vec3 toTarget = target.position().subtract(vehicle.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 side = horizontal.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(horizontal.z, 0.0D, -horizontal.x).normalize();
        double phase = vehicle.tickCount * 0.22D;
        double lateral = Math.sin(phase) * 2.4D;
        double vertical = Math.cos(phase * 0.5D) * 0.45D;
        return target.getEyePosition().add(side.scale(lateral)).add(0.0D, vertical, 0.0D);
    }

    private static TargetVehicleClass targetVehicleClass(Player target) {
        if (!(target.getVehicle() instanceof VehicleEntity targetVehicle)) {
            return TargetVehicleClass.NONE;
        }
        VehicleType type = targetVehicle.vehicleData().defaults().vehicleType();
        if (type == VehicleType.LAND || type == VehicleType.BOAT || type == VehicleType.ARTILLERY) {
            return TargetVehicleClass.SURFACE;
        }
        return TargetVehicleClass.AIR;
    }

    private static void airPatrol(VehicleEntity vehicle, Brain brain, Vec3 anchor, String kind) {
        double desiredAltitude = "mi28".equals(kind) ? 38.0D : 24.0D;
        if (brain.patrolTarget == null || vehicle.position().distanceToSqr(brain.patrolTarget) < 64.0D || vehicle.tickCount % 180 == 0) {
            double angle = vehicle.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = ("mi28".equals(kind) ? 55.0D : 38.0D) + vehicle.getRandom().nextDouble() * 24.0D;
            brain.patrolTarget = anchor.add(Math.cos(angle) * radius, desiredAltitude, Math.sin(angle) * radius);
        }
        flyToward(vehicle, brain, brain.patrolTarget, null, desiredAltitude, true);
    }

    private static void airEngage(VehicleEntity vehicle, Brain brain, Player target, String kind, double distance) {
        double minRange = "mi28".equals(kind) ? 60.0D : 34.0D;
        double maxRange = "mi28".equals(kind) ? 120.0D : 66.0D;
        double orbitRange = "mi28".equals(kind) ? 78.0D : 44.0D;
        double desiredAltitude = "mi28".equals(kind) ? 48.0D : 30.0D;
        Vec3 toTarget = target.position().subtract(vehicle.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        Vec3 direction = horizontal.lengthSqr() < 1.0E-4D ? vehicleForward(vehicle) : horizontal.normalize();
        Vec3 destination;
        if (distance > maxRange) {
            destination = target.position().subtract(direction.scale(orbitRange));
        } else if (distance < minRange) {
            destination = vehicle.position().subtract(direction.scale(40.0D));
            brain.airEvasionTicks = Math.max(brain.airEvasionTicks, 18);
        } else {
            if (vehicle.tickCount % 120 == 0) {
                brain.orbitDirection = -brain.orbitDirection;
            }
            Vec3 strafe = new Vec3(direction.z, 0.0D, -direction.x).scale(orbitRange * brain.orbitDirection);
            destination = target.position().subtract(direction.scale(orbitRange * 0.55D)).add(strafe);
        }
        Vec3 attackFaceTarget = shouldFaceTargetForNoseWeapon(kind, target, distance) ? target.position() : null;
        flyToward(vehicle, brain, destination, attackFaceTarget, desiredAltitude, distance <= maxRange);
    }

    private static boolean shouldFaceTargetForNoseWeapon(String kind, Player target, double distance) {
        if ("ah6".equals(kind)) {
            return distance >= 42.0D && distance <= 82.0D;
        }
        if ("mi28".equals(kind)) {
            return distance >= 55.0D && distance <= 150.0D;
        }
        return false;
    }

    private static void flyToward(VehicleEntity vehicle, Brain brain, Vec3 destination, Vec3 faceTarget, double desiredAltitude, boolean allowBrake) {
        if (brain.airEvasionTicks > 0) {
            brain.airEvasionTicks--;
        }
        double altitude = altitudeAboveTerrain(vehicle);
        boolean climbingOut = altitude < Math.min(desiredAltitude - 4.0D, 18.0D);
        if (climbingOut) {
            float levelPitch = Mth.clamp(-vehicle.getXRot() / 24.0F, -0.45F, 0.45F);
            vehicle.setAiVehicleInput(airInput(false, false, false, false, false, true, false, 0.0F, levelPitch));
            brain.airEvasionTicks = 0;
            return;
        }
        if (isAirForwardUnsafe(vehicle)) {
            brain.airEvasionTicks = Math.max(brain.airEvasionTicks, 28);
        }

        Vec3 toTarget = destination.subtract(vehicle.position());
        double horizontalDistance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        Vec3 yawTarget = faceTarget == null ? destination : faceTarget;
        Vec3 toYawTarget = yawTarget.subtract(vehicle.position());
        double yawDistance = Math.sqrt(toYawTarget.x * toYawTarget.x + toYawTarget.z * toYawTarget.z);
        float desiredYaw = yawDistance < 1.0D ? vehicle.getYRot() : (float) -Math.toDegrees(Math.atan2(toYawTarget.x, toYawTarget.z));
        float yawDiff = Mth.wrapDegrees(desiredYaw - vehicle.getYRot());
        float yawInputScale = faceTarget == null ? 42.0F : 10.0F;
        float mouseX = Mth.clamp(yawDiff / yawInputScale, -1.0F, 1.0F);
        double altitudeError = desiredAltitude - altitude;
        boolean ascend = altitudeError > 3.0D || brain.airEvasionTicks > 0;
        boolean descend = altitudeError < -10.0D && brain.airEvasionTicks <= 0;
        boolean holdingForNoseAim = faceTarget != null && Math.abs(yawDiff) > 4.0F;
        boolean forward = horizontalDistance > 14.0D && Math.abs(yawDiff) < 80.0F && brain.airEvasionTicks <= 0 && !holdingForNoseAim;
        boolean brake = allowBrake && horizontalDistance < 22.0D && Math.abs(yawDiff) < 35.0F;
        float desiredPitch = forward ? 10.0F : 0.0F;
        if (brain.airEvasionTicks > 0) {
            desiredPitch = -8.0F;
        }
        float mouseY = Mth.clamp((desiredPitch - vehicle.getXRot()) / 24.0F, -0.7F, 0.7F);
        vehicle.setAiVehicleInput(airInput(forward, brain.airEvasionTicks > 0, false, false, brake, ascend, descend, mouseX, mouseY));
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

    private static VehicleInput airInput(boolean forward, boolean backward, boolean left, boolean right, boolean brake, boolean ascend, boolean descend, float mouseX, float mouseY) {
        return new VehicleInput(forward, backward, left, right, brake, ascend, descend, false, false, false, false, false, -1, false, false, mouseX, mouseY);
    }

    private static Aim aimAt(VehicleEntity vehicle, LivingEntity target) {
        Vec3 muzzle = vehicle.position().add(0.0D, 2.4D, 0.0D);
        Vec3 targetPos = target.getEyePosition();
        Vec3 delta = targetPos.subtract(muzzle);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float worldYaw = (float) -Math.toDegrees(Math.atan2(delta.x, delta.z));
        float turretYaw = Mth.wrapDegrees(vehicle.getYRot() - worldYaw);
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), landTurretMinPitch(vehicle), landTurretMaxPitch(vehicle));
        return new Aim(worldYaw, turretYaw, pitch);
    }

    private static float landTurretMinPitch(VehicleEntity vehicle) {
        return "bmp2".equals(vehicleKind(vehicle)) ? -74.0F : -15.0F;
    }

    private static float landTurretMaxPitch(VehicleEntity vehicle) {
        return "bmp2".equals(vehicleKind(vehicle)) ? 7.5F : 32.5F;
    }

    private static Aim airAimAt(VehicleEntity vehicle, Vec3 targetPos, String kind) {
        return airAimAt(vehicle, targetPos, kind, null);
    }

    private static Aim airAimAt(VehicleEntity vehicle, Vec3 targetPos, String kind, Vec3 origin) {
        Vec3 muzzle = origin == null ? vehicle.position().add(0.0D, "mi28".equals(kind) ? 1.8D : 1.2D, 0.0D) : origin;
        Vec3 delta = targetPos.subtract(muzzle);
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float worldYaw = (float) -Math.toDegrees(Math.atan2(delta.x, delta.z));
        float turretYaw = Mth.wrapDegrees(vehicle.getYRot() - worldYaw);
        float minPitch = "mi28".equals(kind) ? -10.0F : -45.0F;
        float maxPitch = "mi28".equals(kind) ? 40.0F : 45.0F;
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), minPitch, maxPitch);
        return new Aim(worldYaw, turretYaw, pitch);
    }

    private static double altitudeAboveTerrain(VehicleEntity vehicle) {
        return vehicle.getY() - terrainHeight(vehicle, vehicle.position());
    }

    private static int terrainHeight(VehicleEntity vehicle, Vec3 position) {
        return vehicle.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(position.x), Mth.floor(position.z));
    }

    private static boolean isAirForwardUnsafe(VehicleEntity vehicle) {
        Vec3 forward = vehicleForward(vehicle);
        Vec3 start = vehicle.position().add(0.0D, 1.4D, 0.0D);
        Vec3 end = start.add(forward.scale(18.0D));
        HitResult hit = vehicle.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        if (hit.getType() != HitResult.Type.MISS) {
            return true;
        }
        Vec3 sample = vehicle.position().add(forward.scale(16.0D));
        return sample.y - terrainHeight(vehicle, sample) < 8.0D;
    }

    private static Vec3 vehicleForward(VehicleEntity vehicle) {
        double yaw = Math.toRadians(vehicle.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
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

    private static void aimCrewAt(LivingEntity crew, Aim aim) {
        crew.setYRot(aim.worldYaw());
        crew.setYHeadRot(aim.worldYaw());
        crew.yBodyRot = aim.worldYaw();
        crew.setXRot(aim.pitch());
    }

    private record Aim(float worldYaw, float turretYaw, float pitch) {}

    private enum TargetVehicleClass {
        NONE,
        SURFACE,
        AIR
    }

    private static final class Brain {
        private UUID targetId;
        private int targetMemory;
        private Vec3 patrolTarget;
        private Vec3 lastPosition;
        private int stuckTicks;
        private int reverseTicks;
        private int orbitDirection = 1;
        private int airEvasionTicks;
    }
}
