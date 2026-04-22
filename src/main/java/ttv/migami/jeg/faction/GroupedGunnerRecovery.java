package ttv.migami.jeg.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class GroupedGunnerRecovery {
    private static final int MOB_COOLDOWN_TICKS = 160;
    private static final int GROUP_COOLDOWN_TICKS = 60;
    private static final int MAX_TELEPORTS_PER_MOB = 2;
    private static final int MAX_BATCH_SIZE = 3;
    private static final Map<String, Long> GROUP_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> MOB_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> MOB_TELEPORT_COUNTS = new HashMap<>();

    private GroupedGunnerRecovery() {}

    public static boolean tryRecoverGroundMob(ServerLevel level, String groupKey, BlockPos origin, PathfinderMob mob,
                                              ServerPlayer preferredTarget, int localMinDistance, int localMaxDistance,
                                              int validatedMinDistance, int validatedMaxDistance, int attempts,
                                              int groupEligibleCount) {
        long now = level.getGameTime();
        if (groupEligibleCount <= 0) {
            return false;
        }
        if (isMobCoolingDown(mob.getUUID(), now) || isGroupCoolingDown(groupKey, now)) {
            return false;
        }
        if (MOB_TELEPORT_COUNTS.getOrDefault(mob.getUUID(), 0) >= MAX_TELEPORTS_PER_MOB) {
            return false;
        }
        if (preferredTarget.hasLineOfSight(mob)) {
            return false;
        }

        List<BlockPos> reserved = new ArrayList<>();
        BlockPos recoveryPos = FactionSpawnHelper.findReservedHiddenCombatRecoveryPosition(
                level,
                mob,
                origin,
                preferredTarget,
                mob.getTarget() != null ? mob.getTarget().position() : preferredTarget.position(),
                localMinDistance,
                localMaxDistance,
                validatedMinDistance,
                validatedMaxDistance,
                attempts,
                reserved
        );
        if (recoveryPos == null) {
            return false;
        }

        teleportGroundMob(mob, preferredTarget, recoveryPos);
        markRecovered(groupKey, mob.getUUID(), now);
        return true;
    }

    public static boolean tryRecoverAirMob(ServerLevel level, String groupKey, PathfinderMob mob, Player preferredTarget,
                                           Vec3 recoveryPos, int groupEligibleCount) {
        long now = level.getGameTime();
        if (groupEligibleCount <= 0) {
            return false;
        }
        if (isMobCoolingDown(mob.getUUID(), now) || isGroupCoolingDown(groupKey, now)) {
            return false;
        }
        if (MOB_TELEPORT_COUNTS.getOrDefault(mob.getUUID(), 0) >= MAX_TELEPORTS_PER_MOB) {
            return false;
        }
        if (preferredTarget.hasLineOfSight(mob)) {
            return false;
        }

        mob.getNavigation().stop();
        mob.teleportTo(recoveryPos.x, recoveryPos.y, recoveryPos.z);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
        mob.setTarget(preferredTarget);
        mob.setAggressive(true);
        markRecovered(groupKey, mob.getUUID(), now);
        return true;
    }

    public static int countEligibleGroundMobs(ServerLevel level, List<? extends Mob> mobs, @Nullable Player preferredTarget, double minDistanceSq) {
        int count = 0;
        for (Mob mob : mobs) {
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                continue;
            }
            if (preferredTarget == null || preferredTarget.hasLineOfSight(mob)) {
                continue;
            }
            if (mob.distanceToSqr(preferredTarget) <= minDistanceSq) {
                continue;
            }
            if (!pathfinderMob.getNavigation().isDone() && mob.hasLineOfSight(preferredTarget)) {
                continue;
            }
            count++;
            if (count >= MAX_BATCH_SIZE) {
                return count;
            }
        }
        return count;
    }

    private static void teleportGroundMob(PathfinderMob mob, ServerPlayer preferredTarget, BlockPos recoveryPos) {
        mob.getNavigation().stop();
        mob.teleportTo(recoveryPos.getX() + 0.5D, recoveryPos.getY(), recoveryPos.getZ() + 0.5D);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
        mob.setTarget(preferredTarget);
        mob.setAggressive(true);
        FactionSpawnHelper.moveToTargetWithPathFallback(mob, preferredTarget);
    }

    private static boolean isGroupCoolingDown(String groupKey, long now) {
        Long until = GROUP_COOLDOWNS.get(groupKey);
        return until != null && until > now;
    }

    private static boolean isMobCoolingDown(UUID mobId, long now) {
        Long until = MOB_COOLDOWNS.get(mobId);
        return until != null && until > now;
    }

    private static void markRecovered(String groupKey, UUID mobId, long now) {
        GROUP_COOLDOWNS.put(groupKey, now + GROUP_COOLDOWN_TICKS);
        MOB_COOLDOWNS.put(mobId, now + MOB_COOLDOWN_TICKS);
        MOB_TELEPORT_COUNTS.merge(mobId, 1, Integer::sum);
    }
}
