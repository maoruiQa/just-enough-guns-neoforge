package ttv.migami.jeg.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.event.GunEvents;
import ttv.migami.jeg.init.ModTags;

public final class FactionSpawnHelper {
    public static final String PATROL_TAG = "GunnerPatroller";
    public static final String RAID_TAG = "FactionRaidMob";
    public static final String PATROL_ID_TAG_PREFIX = "JEGPatrolId:";
    public static final String PATROL_FACTION_TAG_PREFIX = "JEGPatrolFaction:";
    public static final String OMEN_FACTION_TAG_PREFIX = "JEGOmenFaction:";
    private static final int MAX_SPAWN_POSITION_ATTEMPTS = 96;
    private static final int MAX_GROUND_SAMPLE_ATTEMPTS = 12;
    private static final int CLUSTER_SPAWN_MAX_OFFSET = 4;
    private static final int PATH_SEARCH_RANGE = 32;
    private static final double NAVIGATION_SPEED = 1.2D;
    private static final double RAID_PLAYER_BUFFER_RADIUS = 20.0D;
    private static final double STUCK_RECOVERY_MIN_TARGET_DISTANCE = 20.0D;
    private static final int[][] TARGET_PATH_OFFSETS = new int[][] {
            {0, 0},
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
            {2, 0}, {-2, 0}, {0, 2}, {0, -2}
    };
    private static volatile String lastPatrolDebug = "no-attempt";

    private FactionSpawnHelper() {}

    public static String getLastPatrolDebug() {
        return lastPatrolDebug;
    }

    @Nullable
    public static Faction getRandomFaction() {
        GunnerManager manager = GunnerManager.getInstance();
        String name = manager.getRandomFactionName();
        return name == null ? null : manager.getFactionByName(name);
    }

    public static List<Mob> spawnPatrol(ServerLevel level, Faction faction, int size, @Nullable Player targetPlayer, BlockPos.MutableBlockPos spawnPos, int spread, boolean forceGuns) {
        List<Mob> spawned = new ArrayList<>();
        String patrolId = UUID.randomUUID().toString();
        RandomSource random = level.getRandom();
        int attempts = 0;
        int maxAttempts = Math.max(64, size * 48);
        int noTargetFailures = 0;
        int createFailures = 0;
        int spawnFailures = 0;
        int pathFailures = 0;
        int borderFailures = 0;
        int feetBlockedFailures = 0;
        int headBlockedFailures = 0;
        int noGroundFailures = 0;
        int fluidFailures = 0;
        int leafBlockedFailures = 0;
        int collisionFailures = 0;
        int addEntityFailures = 0;
        BlockPos clusterCenter = targetPlayer != null
                ? sampleGroundPosition(level, targetPlayer.blockPosition(), random, 12, 28)
                : resolveSurfaceSpawn(level, spawnPos);

        while (spawned.size() < size && attempts++ < maxAttempts) {
            BlockPos candidate = resolvePatrolClusterPosition(level, clusterCenter, spawnPos, random, spread, spawned.isEmpty());
            Player spawnTarget = resolveSpawnTarget(level, candidate, targetPlayer);
            if (spawnTarget == null) {
                noTargetFailures++;
                if (spawned.isEmpty()) {
                    clusterCenter = targetPlayer != null
                            ? sampleGroundPosition(level, targetPlayer.blockPosition(), random, 12, 28)
                            : resolveSurfaceSpawn(level, spawnPos);
                }
                continue;
            }

            Mob mob = createFactionMob(level, faction, candidate, true, forceGuns);
            if (mob == null) {
                createFailures++;
                continue;
            }

            SpawnFailReason spawnResult = prepareAndSpawn(level, mob, candidate, true, false);
            if (spawnResult == SpawnFailReason.NONE) {
                mob.addTag(PATROL_ID_TAG_PREFIX + patrolId);
                mob.addTag(PATROL_FACTION_TAG_PREFIX + faction.getName());
                if (!configurePatrolBehavior(mob, spawnTarget)) {
                    pathFailures++;
                }
                spawned.add(mob);
                continue;
            }

            spawnFailures++;
            switch (spawnResult) {
                case OUT_OF_BORDER -> borderFailures++;
                case FEET_BLOCKED -> feetBlockedFailures++;
                case HEAD_BLOCKED -> headBlockedFailures++;
                case NO_GROUND -> noGroundFailures++;
                case FLUID_BLOCKED -> fluidFailures++;
                case LEAF_BLOCKED -> leafBlockedFailures++;
                case COLLISION_BLOCKED -> collisionFailures++;
                case ADD_ENTITY_FAILED -> addEntityFailures++;
                default -> {
                }
            }
        }

        lastPatrolDebug = "attempts=" + attempts
                + ", requested=" + size
                + ", spawned=" + spawned.size()
                + ", noTarget=" + noTargetFailures
                + ", createFail=" + createFailures
                + ", spawnFail=" + spawnFailures
                + ", pathFail=" + pathFailures
                + ", borderFail=" + borderFailures
                + ", feetBlocked=" + feetBlockedFailures
                + ", headBlocked=" + headBlockedFailures
                + ", noGround=" + noGroundFailures
                + ", fluidFail=" + fluidFailures
                + ", leafBlocked=" + leafBlockedFailures
                + ", collisionFail=" + collisionFailures
                + ", addEntityFail=" + addEntityFailures
                + ", faction=" + faction.getName();
        if (spawned.isEmpty()) {
            JustEnoughGuns.LOGGER.warn("[PatrolDebug] {}", lastPatrolDebug);
        } else {
            // JustEnoughGuns.LOGGER.debug("[PatrolDebug] {}", lastPatrolDebug);
        }

        return spawned;
    }

    public static List<Mob> spawnPatrol(ServerLevel level, Faction faction, int size, Player targetPlayer, BlockPos.MutableBlockPos spawnPos) {
        return spawnPatrol(level, faction, size, targetPlayer, spawnPos, 4, true);
    }

    @Nullable
    public static Mob spawnRaidMember(ServerLevel level, Faction faction, BlockPos origin, @Nullable Player preferredTarget) {
        return spawnRaidMember(level, faction, origin, preferredTarget, null);
    }

    @Nullable
    public static Mob spawnRaidMember(ServerLevel level, Faction faction, BlockPos origin, @Nullable Player preferredTarget, @Nullable BlockPos burstCenter) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < MAX_SPAWN_POSITION_ATTEMPTS; attempt++) {
            BlockPos candidate = burstCenter != null
                    ? sampleClusteredRaidPosition(level, burstCenter, random)
                    : sampleRaidEdgePosition(level, origin, random, 12, 23);
            if (isTooCloseToRaidPlayers(level, candidate)) {
                continue;
            }
            Player target = resolveSpawnTarget(level, candidate, preferredTarget);
            if (target == null) {
                continue;
            }

            Mob mob = createFactionMob(level, faction, candidate, true, true);
            if (mob == null || prepareAndSpawn(level, mob, candidate, false, true) != SpawnFailReason.NONE) {
                continue;
            }
            if (!configureRaidBehavior(mob, target)) {
                // JustEnoughGuns.LOGGER.debug("[PatrolDebug] raid path setup failed for faction={}, mob={}", faction.getName(), mob.getType());
            }

            return mob;
        }

        return null;
    }

    @Nullable
    private static Mob createFactionMob(ServerLevel level, Faction faction, BlockPos spawnPos, boolean canBeElite, boolean forceGuns) {
        List<String> mobPool = faction.getMobs();
        if (mobPool.isEmpty()) {
            return null;
        }

        RandomSource random = level.getRandom();
        String mobId = mobPool.get(random.nextInt(mobPool.size()));
        ResourceLocation entityId = ResourceLocation.tryParse(mobId);
        if (entityId == null) {
            return null;
        }

        Optional<net.minecraft.world.entity.EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId);
        if (type.isEmpty()) {
            return null;
        }

        net.minecraft.world.entity.Entity raw = type.get().create(level);
        if (!(raw instanceof Mob mob)) {
            return null;
        }

        mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        if (shouldTagAsGunner(level, mob, forceGuns)) {
            mob.addTag(GunEvents.JEG_GUNNER_TAG);
        }
        mob.setPersistenceRequired();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
        if (mob.getTags().contains(GunEvents.JEG_GUNNER_TAG)) {
            GunnerMobSpawner.normalizeGunnerMob(mob);
        }

        if (canBeElite && GunMobValues.elitesEnabled && random.nextFloat() < GunMobValues.eliteChance) {
            mob.addTag("EliteGunner");
        }

        long dayTime = level.getDayTime() % 24000L;
        boolean isDay = dayTime >= 0L && dayTime < 12300L;
        if (mob.getType().is(ModTags.Entities.UNDEAD) && mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && isDay) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        }

        return mob;
    }

    private static SpawnFailReason prepareAndSpawn(ServerLevel level, Mob mob, BlockPos spawnPos, boolean patrol, boolean raid) {
        SpawnFailReason spawnCheck = canSpawnAt(level, mob, spawnPos);
        if (spawnCheck != SpawnFailReason.NONE) {
            return spawnCheck;
        }

        if (patrol) {
            mob.addTag(PATROL_TAG);
        }
        if (raid) {
            mob.addTag(RAID_TAG);
        }

        if (!level.addFreshEntity(mob)) {
            return SpawnFailReason.ADD_ENTITY_FAILED;
        }
        return SpawnFailReason.NONE;
    }

    private static boolean shouldTagAsGunner(ServerLevel level, Mob mob, boolean forceGuns) {
        if (forceGuns) {
            return true;
        }

        int currentDay = (int) (level.getDayTime() / 24000L);
        int daysOverMin = Math.max(0, currentDay - GunMobValues.minDays);
        int currentChance = Math.min(GunMobValues.initialChance + (daysOverMin * GunMobValues.chanceIncrement), GunMobValues.maxChance);
        return mob.getRandom().nextInt(100) < currentChance;
    }

    private static BlockPos resolvePatrolClusterPosition(ServerLevel level, BlockPos clusterCenter, BlockPos.MutableBlockPos fallback, RandomSource random, int spread, boolean firstSpawn) {
        if (firstSpawn) {
            return clusterCenter;
        }

        int maxOffset = Math.max(2, Math.min(Math.max(1, spread), CLUSTER_SPAWN_MAX_OFFSET));
        BlockPos candidate = clusterCenter;
        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            int dx = random.nextInt(maxOffset * 2 + 1) - maxOffset;
            int dz = random.nextInt(maxOffset * 2 + 1) - maxOffset;
            candidate = resolveSurfaceSpawn(level, clusterCenter.offset(dx, 0, dz));
            if (isSafeGroundPosition(level, candidate)) {
                return candidate;
            }
        }

        return resolveSurfaceSpawn(level, fallback);
    }

    private static BlockPos sampleClusteredRaidPosition(ServerLevel level, BlockPos burstCenter, RandomSource random) {
        BlockPos center = resolveSurfaceSpawn(level, burstCenter);
        if (isSafeGroundPosition(level, center)) {
            return center;
        }

        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            int dx = random.nextInt(CLUSTER_SPAWN_MAX_OFFSET * 2 + 1) - CLUSTER_SPAWN_MAX_OFFSET;
            int dz = random.nextInt(CLUSTER_SPAWN_MAX_OFFSET * 2 + 1) - CLUSTER_SPAWN_MAX_OFFSET;
            BlockPos candidate = resolveSurfaceSpawn(level, burstCenter.offset(dx, 0, dz));
            if (isSafeGroundPosition(level, candidate)) {
                return candidate;
            }
        }

        return center;
    }

    public static @Nullable BlockPos resolveRaidBurstCenter(ServerLevel level, BlockPos origin, @Nullable BlockPos burstCenter,
                                                            RandomSource random, int minRadius, int maxRadius) {
        if (burstCenter != null) {
            BlockPos resolved = resolveSurfaceSpawn(level, burstCenter);
            if (isSafeGroundPosition(level, resolved) && !isTooCloseToRaidPlayers(level, resolved)) {
                return resolved;
            }
        }

        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            BlockPos candidate = sampleRaidEdgePosition(level, origin, random, minRadius, maxRadius);
            if (!isSafeGroundPosition(level, candidate) || isTooCloseToRaidPlayers(level, candidate)) {
                continue;
            }
            return candidate;
        }

        return null;
    }

    private static @Nullable Player resolveSpawnTarget(ServerLevel level, BlockPos spawnPos, @Nullable Player preferredTarget) {
        if (isValidTarget(level, preferredTarget)) {
            return preferredTarget;
        }

        Player nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        double sx = spawnPos.getX() + 0.5D;
        double sy = spawnPos.getY() + 0.5D;
        double sz = spawnPos.getZ() + 0.5D;

        for (Player player : level.players()) {
            if (!isValidTarget(level, player)) {
                continue;
            }

            double distanceSq = player.distanceToSqr(sx, sy, sz);
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = player;
            }
        }

        return nearest;
    }

    private static boolean isValidTarget(ServerLevel level, @Nullable Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return serverPlayer.level() == level
                && serverPlayer.isAlive()
                && !serverPlayer.isSpectator()
                && serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
    }

    public static boolean isTooCloseToRaidPlayers(ServerLevel level, BlockPos pos) {
        double minDistanceSq = RAID_PLAYER_BUFFER_RADIUS * RAID_PLAYER_BUFFER_RADIUS;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        for (Player player : level.players()) {
            if (!isValidTarget(level, player)) {
                continue;
            }
            if (player.distanceToSqr(x, y, z) < minDistanceSq) {
                return true;
            }
        }
        return false;
    }

    public static boolean moveToTargetWithPathFallback(PathfinderMob pathfinderMob, Player targetPlayer) {
        Path directPath = pathfinderMob.getNavigation().createPath(targetPlayer, PATH_SEARCH_RANGE);
        if (directPath != null && pathfinderMob.getNavigation().moveTo(directPath, NAVIGATION_SPEED)) {
            return true;
        }

        BlockPos targetPos = targetPlayer.blockPosition();
        for (int[] offset : TARGET_PATH_OFFSETS) {
            BlockPos nearby = targetPos.offset(offset[0], 0, offset[1]);
            Path nearbyPath = pathfinderMob.getNavigation().createPath(nearby, PATH_SEARCH_RANGE);
            if (nearbyPath != null && pathfinderMob.getNavigation().moveTo(nearbyPath, NAVIGATION_SPEED)) {
                return true;
            }
        }

        return false;
    }

    public static int updateStationaryTicks(Map<UUID, Vec3> lastPositions, Map<UUID, Integer> stationaryTicks, UUID mobId, Vec3 currentPos, int tickStep, double movementThresholdSq) {
        Vec3 lastPos = lastPositions.put(mobId, currentPos);
        if (lastPos == null || lastPos.distanceToSqr(currentPos) >= movementThresholdSq) {
            stationaryTicks.remove(mobId);
            return 0;
        }
        return stationaryTicks.merge(mobId, tickStep, Integer::sum);
    }

    public static boolean trySoftTargetRecovery(PathfinderMob pathfinderMob, @Nullable Player targetPlayer) {
        pathfinderMob.getNavigation().stop();
        if (targetPlayer == null) {
            return false;
        }
        pathfinderMob.setTarget(targetPlayer);
        return moveToTargetWithPathFallback(pathfinderMob, targetPlayer);
    }

    private static boolean configurePatrolBehavior(Mob mob, @Nullable Player targetPlayer) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return true;
        }

        if (targetPlayer == null) {
            return true;
        }

        pathfinderMob.setTarget(targetPlayer);
        if (pathfinderMob.getNavigation().moveTo(targetPlayer, NAVIGATION_SPEED)) {
            return true;
        }

        return moveToTargetWithPathFallback(pathfinderMob, targetPlayer);
    }

    private static boolean configureRaidBehavior(Mob mob, Player targetPlayer) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return true;
        }

        pathfinderMob.setTarget(targetPlayer);
        if (pathfinderMob.getNavigation().moveTo(targetPlayer, NAVIGATION_SPEED)) {
            return true;
        }

        return moveToTargetWithPathFallback(pathfinderMob, targetPlayer);
    }

    @Nullable
    public static BlockPos findLocalGroundRecoveryPosition(ServerLevel level, PathfinderMob mob, BlockPos anchor, @Nullable Player targetPlayer,
                                                           int minDistance, int maxDistance, int attempts, boolean raidPlayerBuffer) {
        double maxAnchorDistanceSq = (double) maxDistance * (double) maxDistance;
        BlockPos targetPos = targetPlayer != null ? targetPlayer.blockPosition() : anchor;
        double minTargetDistanceSq = targetPlayer != null
                ? STUCK_RECOVERY_MIN_TARGET_DISTANCE * STUCK_RECOVERY_MIN_TARGET_DISTANCE
                : -1.0D;
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos candidate = sampleGroundPosition(level, anchor, random, minDistance, maxDistance);
            if (!isSafeGroundPosition(level, candidate)) {
                continue;
            }
            if (raidPlayerBuffer && isTooCloseToRaidPlayers(level, candidate)) {
                continue;
            }
            if (candidate.distSqr(anchor) > maxAnchorDistanceSq) {
                continue;
            }
            if (targetPlayer != null && candidate.distSqr(targetPos) <= minTargetDistanceSq) {
                continue;
            }
            if (!canNavigateFrom(level, mob, candidate, targetPlayer)) {
                continue;
            }
            return candidate;
        }

        return null;
    }

    @Nullable
    public static BlockPos findValidatedGroundRecoveryPosition(ServerLevel level, PathfinderMob mob, BlockPos origin, Player targetPlayer,
                                                               int minDistance, int maxDistance, int attempts, boolean raidPlayerBuffer) {
        double minTargetDistanceSq = STUCK_RECOVERY_MIN_TARGET_DISTANCE * STUCK_RECOVERY_MIN_TARGET_DISTANCE;
        double maxOriginDistanceSq = (double) maxDistance * (double) maxDistance;
        BlockPos targetPos = targetPlayer.blockPosition();
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos candidate = sampleGroundPosition(level, origin, random, minDistance, maxDistance);
            if (!isSafeGroundPosition(level, candidate)) {
                continue;
            }
            if (raidPlayerBuffer && isTooCloseToRaidPlayers(level, candidate)) {
                continue;
            }
            if (candidate.distSqr(targetPos) <= minTargetDistanceSq) {
                continue;
            }
            if (candidate.distSqr(origin) > maxOriginDistanceSq) {
                continue;
            }
            if (!canNavigateFrom(level, mob, candidate, targetPlayer)) {
                continue;
            }
            return candidate;
        }

        for (int attempt = 0; attempt < attempts; attempt++) {
            BlockPos candidate = sampleRaidEdgePosition(level, origin, random, minDistance, maxDistance);
            if (!isSafeGroundPosition(level, candidate)) {
                continue;
            }
            if (raidPlayerBuffer && isTooCloseToRaidPlayers(level, candidate)) {
                continue;
            }
            if (candidate.distSqr(targetPos) <= minTargetDistanceSq) {
                continue;
            }
            if (candidate.distSqr(origin) > maxOriginDistanceSq) {
                continue;
            }
            if (!canNavigateFrom(level, mob, candidate, targetPlayer)) {
                continue;
            }
            return candidate;
        }

        return null;
    }

    @Nullable
    public static BlockPos findHiddenCombatRecoveryPosition(ServerLevel level, PathfinderMob mob, BlockPos origin, Player targetPlayer,
                                                            @Nullable Vec3 lastKnownPosition, int localMinDistance, int localMaxDistance,
                                                            int validatedMinDistance, int validatedMaxDistance, int attempts) {
        return findReservedHiddenCombatRecoveryPosition(level, mob, origin, targetPlayer, lastKnownPosition,
                localMinDistance, localMaxDistance, validatedMinDistance, validatedMaxDistance, attempts, List.of());
    }

    public static BlockPos findReservedHiddenCombatRecoveryPosition(ServerLevel level, PathfinderMob mob, BlockPos origin, Player targetPlayer,
                                                                    @Nullable Vec3 lastKnownPosition, int localMinDistance, int localMaxDistance,
                                                                    int validatedMinDistance, int validatedMaxDistance, int attempts,
                                                                    List<BlockPos> reservedPositions) {
        BlockPos candidate = findHiddenCombatRecoveryPositionAround(level, mob, origin, targetPlayer, localMinDistance, localMaxDistance, attempts, true, reservedPositions);
        if (candidate != null) {
            return candidate;
        }

        candidate = findHiddenCombatRecoveryPositionAround(level, mob, origin, targetPlayer, validatedMinDistance, validatedMaxDistance, attempts, false, reservedPositions);
        if (candidate != null) {
            return candidate;
        }

        if (lastKnownPosition != null) {
            BlockPos lastKnownPos = BlockPos.containing(lastKnownPosition);
            if (lastKnownPos.distManhattan(origin) >= 6) {
                candidate = findHiddenCombatRecoveryPositionAround(level, mob, lastKnownPos, targetPlayer, validatedMinDistance, validatedMaxDistance, attempts, false, reservedPositions);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    @Nullable
    private static BlockPos findHiddenCombatRecoveryPositionAround(ServerLevel level, PathfinderMob mob, BlockPos anchor, Player targetPlayer,
                                                                   int minDistance, int maxDistance, int attempts, boolean localOnly,
                                                                   List<BlockPos> reservedPositions) {
        if (localOnly) {
            BlockPos candidate = findLocalGroundRecoveryPosition(level, mob, anchor, targetPlayer, minDistance, maxDistance, attempts, false);
            return isHiddenCombatRecoveryCandidate(level, mob, targetPlayer, candidate, reservedPositions) ? candidate : null;
        }

        BlockPos candidate = findValidatedGroundRecoveryPosition(level, mob, anchor, targetPlayer, minDistance, maxDistance, attempts, false);
        return isHiddenCombatRecoveryCandidate(level, mob, targetPlayer, candidate, reservedPositions) ? candidate : null;
    }

    private static boolean isHiddenCombatRecoveryCandidate(ServerLevel level, PathfinderMob mob, Player targetPlayer, @Nullable BlockPos candidate,
                                                           List<BlockPos> reservedPositions) {
        if (candidate == null || !isSafeGroundPosition(level, candidate)) {
            return false;
        }
        for (BlockPos reserved : reservedPositions) {
            if (reserved != null && candidate.closerThan(reserved, 3.0D)) {
                return false;
            }
        }
        if (!canNavigateFrom(level, mob, candidate, targetPlayer)) {
            return false;
        }
        return !hasImmediateLineOfSight(level, candidate, targetPlayer);
    }

    private static boolean hasImmediateLineOfSight(ServerLevel level, BlockPos candidate, Player targetPlayer) {
        Vec3 from = Vec3.atCenterOf(candidate).add(0.0D, 1.4D, 0.0D);
        Vec3 to = targetPlayer.getEyePosition();
        BlockHitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, targetPlayer));
        return result.getType() == HitResult.Type.MISS;
    }

    private static boolean canNavigateFrom(ServerLevel level, PathfinderMob mob, BlockPos spawnPos, Player targetPlayer) {
        double originalX = mob.getX();
        double originalY = mob.getY();
        double originalZ = mob.getZ();
        LivingEntity originalTarget = mob.getTarget();
        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        mob.getNavigation().stop();
        mob.setPos(x, y, z);
        if (!level.noCollision(mob)) {
            mob.setPos(originalX, originalY, originalZ);
            mob.setTarget(originalTarget);
            return false;
        }

        boolean canNavigate = moveToTargetWithPathFallback(mob, targetPlayer);
        mob.getNavigation().stop();
        mob.setPos(originalX, originalY, originalZ);
        mob.setTarget(originalTarget);
        return canNavigate;
    }

    private static SpawnFailReason canSpawnAt(ServerLevel level, Mob mob, BlockPos spawnPos) {
        if (!level.getWorldBorder().isWithinBounds(spawnPos)) {
            return SpawnFailReason.OUT_OF_BORDER;
        }
        BlockState feetState = level.getBlockState(spawnPos);
        BlockState headState = level.getBlockState(spawnPos.above());
        BlockState groundState = level.getBlockState(spawnPos.below());
        if (isLeafBlock(feetState) || isLeafBlock(headState) || isLeafBlock(groundState)) {
            return SpawnFailReason.LEAF_BLOCKED;
        }
        if (!feetState.getCollisionShape(level, spawnPos).isEmpty()) {
            return SpawnFailReason.FEET_BLOCKED;
        }
        if (!headState.getCollisionShape(level, spawnPos.above()).isEmpty()) {
            return SpawnFailReason.HEAD_BLOCKED;
        }
        if (groundState.isAir()) {
            return SpawnFailReason.NO_GROUND;
        }
        if (!level.getFluidState(spawnPos).isEmpty() || !level.getFluidState(spawnPos.above()).isEmpty()) {
            return SpawnFailReason.FLUID_BLOCKED;
        }
        if (!level.noCollision(mob)) {
            return SpawnFailReason.COLLISION_BLOCKED;
        }
        return SpawnFailReason.NONE;
    }

    public static BlockPos sampleGroundPosition(ServerLevel level, BlockPos origin, RandomSource random, int minDistance, int maxDistance) {
        BlockPos fallback = resolveSurfaceSpawn(level, origin);
        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            int dx = random.nextInt(maxDistance - minDistance + 1) + minDistance;
            int dz = random.nextInt(maxDistance - minDistance + 1) + minDistance;
            if (random.nextBoolean()) {
                dx = -dx;
            }
            if (random.nextBoolean()) {
                dz = -dz;
            }

            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            BlockPos candidate = resolveSurfaceSpawn(level, new BlockPos(x, origin.getY(), z));
            if (isSafeGroundPosition(level, candidate)) {
                return candidate;
            }
            fallback = candidate;
        }
        return fallback;
    }

    public static BlockPos sampleRaidEdgePosition(ServerLevel level, BlockPos origin, RandomSource random, int minRadius, int maxRadius) {
        BlockPos fallback = resolveSurfaceSpawn(level, origin);
        for (int attempt = 0; attempt < MAX_GROUND_SAMPLE_ATTEMPTS; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            int radius = random.nextInt(maxRadius - minRadius + 1) + minRadius;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = origin.getZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos candidate = resolveSurfaceSpawn(level, new BlockPos(x, origin.getY(), z));
            if (isSafeGroundPosition(level, candidate)) {
                return candidate;
            }
            fallback = candidate;
        }
        return fallback;
    }

    private static BlockPos resolveSurfaceSpawn(ServerLevel level, BlockPos source) {
        int x = source.getX();
        int z = source.getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    public static boolean isSafeGroundPosition(ServerLevel level, BlockPos spawnPos) {
        if (!level.getWorldBorder().isWithinBounds(spawnPos)) {
            return false;
        }

        BlockState feetState = level.getBlockState(spawnPos);
        BlockState headState = level.getBlockState(spawnPos.above());
        BlockState groundState = level.getBlockState(spawnPos.below());
        if (isLeafBlock(feetState) || isLeafBlock(headState) || isLeafBlock(groundState)) {
            return false;
        }
        if (!feetState.getCollisionShape(level, spawnPos).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(level, spawnPos.above()).isEmpty()) {
            return false;
        }
        if (groundState.isAir()) {
            return false;
        }
        if (!level.getFluidState(spawnPos).isEmpty() || !level.getFluidState(spawnPos.above()).isEmpty()) {
            return false;
        }
        return true;
    }

    private static boolean isLeafBlock(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private enum SpawnFailReason {
        NONE,
        OUT_OF_BORDER,
        FEET_BLOCKED,
        HEAD_BLOCKED,
        NO_GROUND,
        FLUID_BLOCKED,
        LEAF_BLOCKED,
        COLLISION_BLOCKED,
        ADD_ENTITY_FAILED
    }
}
