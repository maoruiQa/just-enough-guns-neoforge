package ttv.migami.jeg.faction.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GroupedGunnerRecovery;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.init.ModEntities;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.util.LootUtils;

public final class FactionRaidManager {
    private static final String RAID_ID_TAG_PREFIX = "JEGFactionRaidId:";
    private static final String RAID_ORIGIN_TAG_PREFIX = "JEGFactionRaidOrigin:";
    private static final String RAID_FACTION_TAG_PREFIX = "JEGFactionRaidFaction:";
    private static final String RAID_TOTAL_WAVES_TAG_PREFIX = "JEGFactionRaidTotalWaves:";
    private static final String RAID_CURRENT_WAVE_TAG_PREFIX = "JEGFactionRaidCurrentWave:";
    private static final String RAID_TARGET_TAG_PREFIX = "JEGFactionRaidTarget:";
    private static final String RAID_FORCE_GUNS_TAG_PREFIX = "JEGFactionRaidForceGuns:";
    private static final int ACTIVE_RADIUS = 64;
    private static final int MAX_ACTIVE_MOBS = 10;
    private static final int WAVE_MOBS = 20;
    private static final int DEFAULT_TOTAL_WAVES = 3;
    private static final int SPAWN_INTERVAL_TICKS = 30;
    private static final int WAVE_COOLDOWN_TICKS = 120;
    private static final int RAID_MANAGER_TICK_INTERVAL = 20;
    private static final int LEGACY_RECOVERY_MERGE_DISTANCE = 128;
    private static final int RECOVERY_RECONCILE_TICKS = 40;
    private static final int STATIONARY_STUCK_TICKS = 120;
    private static final int SPIN_STUCK_TICKS = 120;
    private static final int DISTANCE_STALL_TICKS = 120;
    private static final double STATIONARY_MOVEMENT_THRESHOLD_SQ = 0.25D;
    private static final float SPIN_STUCK_YAW_DELTA_DEGREES = 35.0F;
    private static final double DISTANCE_STALL_THRESHOLD = 1.5D;
    private static final int RAID_RECOVERY_MIN_RADIUS = 12;
    private static final int RAID_RECOVERY_MAX_RADIUS = 23;
    private static final int RAID_BURST_SIZE_MIN = 3;
    private static final int RAID_BURST_SIZE_MAX = 5;
    private static final int RAID_BURST_DELAY_TICKS = 80;
    private static final int LOW_MOB_NO_FIRE_CLEANUP_THRESHOLD = 3;
    private static final int LOW_MOB_NO_FIRE_TIMEOUT_TICKS = 400;
    private static final int REWARD_MARKER_TICK_INTERVAL = 6;
    private static final ResourceKey<LootTable> FACTION_RAID_REWARD_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Reference.id("chests/faction_raid_reward"));
    private static final Map<ServerLevel, List<RaidContext>> ACTIVE_RAIDS = new HashMap<>();

    private FactionRaidManager() {}

    public static boolean hasActiveRaidNear(ServerLevel level, BlockPos pos, double radius) {
        List<RaidContext> raids = ACTIVE_RAIDS.get(level);
        if (raids != null) {
            double radiusSq = radius * radius;
            for (RaidContext raid : raids) {
                if (raid.finished) {
                    continue;
                }
                if (raid.origin.distSqr(pos) <= radiusSq) {
                    return true;
                }
            }
        }

        AABB area = new AABB(
                pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
                pos.getX() + radius, pos.getY() + radius, pos.getZ() + radius
        );
        return !level.getEntitiesOfClass(RaidEntity.class, area, raid -> !raid.isFinishedState()).isEmpty();
    }

    public static void startRaid(ServerLevel level, Faction faction, Vec3 startPos, boolean forceGuns) {
        if (faction == null) {
            return;
        }

        BlockPos origin = BlockPos.containing(startPos);
        if (hasActiveRaidNear(level, origin, ACTIVE_RADIUS)) {
            return;
        }

        RaidContext raid = new RaidContext(UUID.randomUUID(), origin, faction.getName(), forceGuns, DEFAULT_TOTAL_WAVES);
        ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(raid);
        ensureAnchor(level, raid, startPos);

        Component message = Component.translatable(
                "broadcast.jeg.raid",
                Component.translatable("faction.jeg." + faction.getName()),
                origin.toShortString()
        ).withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }

    public static void tickAll(MinecraftServer server) {
        Iterator<Map.Entry<ServerLevel, List<RaidContext>>> levelIterator = ACTIVE_RAIDS.entrySet().iterator();
        while (levelIterator.hasNext()) {
            Map.Entry<ServerLevel, List<RaidContext>> entry = levelIterator.next();
            ServerLevel level = entry.getKey();
            if (level.getServer() != server) {
                continue;
            }

            List<RaidContext> raids = entry.getValue();
            Iterator<RaidContext> raidIterator = raids.iterator();
            while (raidIterator.hasNext()) {
                RaidContext raid = raidIterator.next();
                if (tickRaid(level, raid)) {
                    raidIterator.remove();
                }
            }

            if (raids.isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    public static void recoverRaidAnchor(RaidEntity anchor) {
        if (!(anchor.level() instanceof ServerLevel level)) {
            return;
        }

        List<RaidContext> raids = ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        UUID raidId = anchor.getRaidId();
        RaidContext raid = raidId != null ? findContextById(raids, raidId) : null;
        if (raid == null) {
            raid = findCompatibleLegacyContext(raids, anchor.blockPosition(), anchor.getFactionName());
        }
        if (raid == null) {
            raid = new RaidContext(
                    raidId != null ? raidId : UUID.randomUUID(),
                    anchor.blockPosition(),
                    anchor.getFactionName(),
                    anchor.isForceGuns(),
                    anchor.getTotalWaves()
            );
            raids.add(raid);
        }

        raid.currentWave = Math.max(anchor.getCurrentWave(), raid.currentWave);
        raid.waveCooldown = anchor.getWaveCooldown();
        raid.spawningWave = anchor.isSpawningWave();
        raid.spawnedThisWaveCount = Math.max(anchor.getSpawnedThisWaveCount(), raid.spawnedThisWaveCount);
        raid.finished = anchor.isFinishedState();
        raid.victory = anchor.isVictoryState();
        raid.defeat = anchor.isDefeatState();
        raid.failedNoTargets = anchor.isFailedNoTargetsState();
        raid.rewardGranted = anchor.isRewardGrantedState();
        raid.anchorId = anchor.getUUID();

        anchor.bindToRaid(raid.raidId, raid.factionName, raid.forceGuns, raid.totalWaves, raid.currentWave);
        anchor.syncFromManager(
                raid.factionName,
                raid.currentWave,
                raid.totalWaves,
                raid.spawningWave,
                raid.spawnedThisWaveCount,
                raid.activeMobIds.size(),
                raid.waveCooldown,
                raid.finished,
                raid.victory,
                raid.defeat,
                raid.failedNoTargets,
                raid.rewardGranted
        );
    }

    public static void recoverRaidMob(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!mob.getTags().contains(FactionSpawnHelper.RAID_TAG)) {
            return;
        }

        UUID raidId = parseUuid(readTagValue(mob, RAID_ID_TAG_PREFIX));
        if (raidId == null) {
            return;
        }

        List<RaidContext> raids = ACTIVE_RAIDS.computeIfAbsent(level, ignored -> new ArrayList<>());
        RaidContext raid = findContextById(raids, raidId);
        if (raid == null) {
            return;
        }
        if (raid.finished) {
            mob.discard();
            return;
        }

        raid.trackRecoveredMob(mob);
    }

    public static void notifyRaidMobFired(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        UUID raidId = parseUuid(readTagValue(mob, RAID_ID_TAG_PREFIX));
        if (raidId == null) {
            return;
        }
        List<RaidContext> raids = ACTIVE_RAIDS.get(level);
        if (raids == null) {
            return;
        }
        RaidContext raid = findContextById(raids, raidId);
        if (raid == null || raid.finished) {
            return;
        }
        raid.lowMobNoFireTicks = 0;
    }

    private static boolean tickRaid(ServerLevel level, RaidContext raid) {
        if (raid.finished) {
            tickRewardMarkers(level, raid);
            RaidEntity anchor = findAnchor(level, raid);
            if (anchor == null || anchor.isRemoved() || !anchor.isAlive()) {
                return true;
            }
            syncAnchor(anchor, raid);
            return false;
        }

        if (--raid.reconcileCooldown <= 0) {
            reconcileLoadedRaidMobs(level, raid);
            raid.reconcileCooldown = RECOVERY_RECONCILE_TICKS;
        }

        raid.refreshActiveMobs(level);
        RaidEntity anchor = ensureAnchor(level, raid, Vec3.atCenterOf(raid.origin));

        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            finishDefeat(level, raid, false);
            if (anchor != null) {
                syncAnchor(anchor, raid);
            }
            return false;
        }

        Player preferred = pickPreferredTarget(level, raid);
        if (preferred != null) {
            raid.hadPlayersInRange = true;
            raid.participantPlayerIds.add(preferred.getUUID());
            maintainRaidMobPressure(level, raid, preferred);
            tickActiveRaid(level, raid, preferred);
        } else if (raid.hadPlayersInRange) {
            finishDefeat(level, raid, true);
        }

        if (anchor != null) {
            syncAnchor(anchor, raid);
        }
        return false;
    }

    private static void tickActiveRaid(ServerLevel level, RaidContext raid, Player preferredTarget) {
        if (!raid.spawningWave && raid.spawnedThisWaveCount == 0) {
            raid.waveCooldown -= RAID_MANAGER_TICK_INTERVAL;
            if (raid.waveCooldown <= 0) {
                if (raid.currentWave >= raid.totalWaves) {
                    finishVictory(level, raid);
                } else {
                    startWave(level, raid);
                }
            }
            return;
        }

        if (raid.spawningWave) {
            raid.spawnCooldown -= RAID_MANAGER_TICK_INTERVAL;
            if (raid.spawnCooldown <= 0) {
                raid.spawnCooldown = RAID_BURST_DELAY_TICKS;
                spawnWaveBatch(level, raid, preferredTarget);
                if (raid.spawnedThisWaveCount >= WAVE_MOBS) {
                    raid.spawningWave = false;
                }
            }
        }

        boolean waveSpawnedAll = raid.spawnedThisWaveCount >= WAVE_MOBS;
        if (!raid.spawningWave && waveSpawnedAll && raid.activeMobIds.size() <= LOW_MOB_NO_FIRE_CLEANUP_THRESHOLD) {
            raid.lowMobNoFireTicks += RAID_MANAGER_TICK_INTERVAL;
            if (raid.lowMobNoFireTicks >= LOW_MOB_NO_FIRE_TIMEOUT_TICKS) {
                clearActiveWaveMobs(level, raid);
                raid.lowMobNoFireTicks = 0;
            }
        } else {
            raid.lowMobNoFireTicks = 0;
        }

        if (!raid.spawningWave && waveSpawnedAll && raid.activeMobIds.isEmpty()) {
            if (raid.currentWave >= raid.totalWaves) {
                finishVictory(level, raid);
            } else {
                raid.spawnedThisWaveCount = 0;
                raid.waveCooldown = WAVE_COOLDOWN_TICKS;
                level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("broadcast.jeg.raid.next_wave"), false);
            }
        }
    }

    private static void startWave(ServerLevel level, RaidContext raid) {
        raid.currentWave++;
        raid.spawningWave = true;
        raid.spawnCooldown = 1;
        raid.lowMobNoFireTicks = 0;
        raid.currentBurstCenter = null;
        playHorn(level, raid.origin, false);
    }

    private static void spawnWaveBatch(ServerLevel level, RaidContext raid, Player preferredTarget) {
        Faction faction = resolveFaction(raid.factionName);
        if (faction == null) {
            return;
        }

        if (raid.currentBurstCenter == null || raid.spawnedInCurrentBurst <= 0) {
            raid.spawnedInCurrentBurst = raid.burstSize(level.getRandom());
        }
        raid.currentBurstCenter = FactionSpawnHelper.resolveRaidBurstCenter(
                level,
                raid.origin,
                raid.currentBurstCenter,
                level.getRandom(),
                RAID_RECOVERY_MIN_RADIUS,
                RAID_RECOVERY_MAX_RADIUS
        );
        if (raid.currentBurstCenter == null) {
            return;
        }

        int burstRemaining = Math.min(raid.spawnedInCurrentBurst, WAVE_MOBS - raid.spawnedThisWaveCount);
        int attempts = 0;
        int spawned = 0;
        while (spawned < burstRemaining && raid.activeMobIds.size() < MAX_ACTIVE_MOBS && attempts++ < burstRemaining * 4) {
            Mob mob = FactionSpawnHelper.spawnRaidMember(level, faction, raid.origin, preferredTarget, raid.currentBurstCenter);
            if (mob == null) {
                continue;
            }
            raid.trackSpawn(mob, preferredTarget);
            spawned++;
        }

        raid.spawnedInCurrentBurst = Math.max(0, raid.spawnedInCurrentBurst - spawned);
        if (raid.spawnedInCurrentBurst <= 0) {
            raid.currentBurstCenter = null;
        }
    }

    private static void maintainRaidMobPressure(ServerLevel level, RaidContext raid, Player preferredTarget) {
        Set<UUID> retiredMobs = new HashSet<>();
        for (UUID mobId : new HashSet<>(raid.activeMobIds)) {
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) {
                retiredMobs.add(mobId);
                raid.clearMobTracking(mobId);
                continue;
            }
            if (!mob.getTags().contains(FactionSpawnHelper.RAID_TAG)) {
                retiredMobs.add(mobId);
                raid.clearMobTracking(mobId);
                continue;
            }
            if (!(mob instanceof PathfinderMob pathfinderMob)) {
                raid.clearMobTracking(mobId);
                continue;
            }

            LivingEntity currentTarget = mob.getTarget();
            if (!(currentTarget instanceof Player currentPlayer) || !isValidRaidTarget(currentPlayer, level, raid.origin)) {
                mob.setTarget(preferredTarget);
                mob.setAggressive(true);
                FactionSpawnHelper.moveToTargetWithPathFallback(pathfinderMob, preferredTarget);
            }

            int stationaryTicks = updateStationaryTicks(raid, mobId, mob);
            int spinTicks = updateSpinTicks(raid, mobId, mob);
            int distanceStallTicks = updateDistanceStallTicks(raid, mobId, mob, preferredTarget);
            int stuckSeverity = Math.max(stationaryTicks, Math.max(spinTicks, distanceStallTicks));
            if (stuckSeverity < 60) {
                continue;
            }
            if (stuckSeverity < STATIONARY_STUCK_TICKS) {
                FactionSpawnHelper.trySoftTargetRecovery(pathfinderMob, preferredTarget);
                continue;
            }

            int eligibleCount = Math.min(3, countEligibleRecoveryMobs(raid, level, preferredTarget));
            if (GroupedGunnerRecovery.tryRecoverGroundMob(
                    level,
                    "faction-raid:" + raid.raidId,
                    raid.currentBurstCenter != null ? raid.currentBurstCenter : raid.origin,
                    pathfinderMob,
                    (ServerPlayer) preferredTarget,
                    RAID_RECOVERY_MIN_RADIUS,
                    Math.min(RAID_RECOVERY_MAX_RADIUS, 12),
                    RAID_RECOVERY_MIN_RADIUS,
                    RAID_RECOVERY_MAX_RADIUS,
                    12,
                    eligibleCount
            )) {
                raid.clearMobTracking(mobId);
                continue;
            }

            FactionSpawnHelper.trySoftTargetRecovery(pathfinderMob, preferredTarget);
        }

        if (!retiredMobs.isEmpty()) {
            raid.activeMobIds.removeAll(retiredMobs);
        }
    }

    private static int updateStationaryTicks(RaidContext raid, UUID mobId, Mob mob) {
        Vec3 currentPos = mob.position();
        Vec3 lastPos = raid.lastPositions.put(mobId, currentPos);
        if (lastPos == null || lastPos.distanceToSqr(currentPos) >= STATIONARY_MOVEMENT_THRESHOLD_SQ) {
            raid.stationaryTicks.remove(mobId);
            return 0;
        }
        return raid.stationaryTicks.merge(mobId, RAID_MANAGER_TICK_INTERVAL, Integer::sum);
    }

    private static int updateSpinTicks(RaidContext raid, UUID mobId, Mob mob) {
        float currentYaw = mob.getYRot();
        Float lastYaw = raid.lastYaws.put(mobId, currentYaw);
        if (lastYaw == null) {
            raid.spinTicks.remove(mobId);
            return 0;
        }
        float delta = Math.abs(Mth.wrapDegrees(currentYaw - lastYaw));
        if (delta < SPIN_STUCK_YAW_DELTA_DEGREES) {
            raid.spinTicks.remove(mobId);
            return 0;
        }
        return raid.spinTicks.merge(mobId, RAID_MANAGER_TICK_INTERVAL, Integer::sum);
    }

    private static int updateDistanceStallTicks(RaidContext raid, UUID mobId, Mob mob, Player preferredTarget) {
        double distanceSq = mob.distanceToSqr(preferredTarget);
        Double lastDistanceSq = raid.lastTargetDistanceSq.put(mobId, distanceSq);
        if (lastDistanceSq == null || lastDistanceSq - distanceSq > DISTANCE_STALL_THRESHOLD) {
            raid.distanceStallTicks.remove(mobId);
            return 0;
        }
        return raid.distanceStallTicks.merge(mobId, RAID_MANAGER_TICK_INTERVAL, Integer::sum);
    }

    private static int countEligibleRecoveryMobs(RaidContext raid, ServerLevel level, Player preferredTarget) {
        int eligible = 0;
        for (UUID id : raid.activeMobIds) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !(mob instanceof PathfinderMob)) {
                continue;
            }
            if (preferredTarget.hasLineOfSight(mob)) {
                continue;
            }
            int severity = Math.max(
                    raid.stationaryTicks.getOrDefault(id, 0),
                    Math.max(raid.spinTicks.getOrDefault(id, 0), raid.distanceStallTicks.getOrDefault(id, 0))
            );
            if (severity >= STATIONARY_STUCK_TICKS) {
                eligible++;
                if (eligible >= 3) {
                    return eligible;
                }
            }
        }
        return eligible;
    }

    @Nullable
    private static Mob recreateRaidMob(ServerLevel level, RaidContext raid, PathfinderMob mob, Player preferredTarget) {
        BlockPos recoveryPos = FactionSpawnHelper.findValidatedGroundRecoveryPosition(
                level,
                mob,
                raid.origin,
                preferredTarget,
                RAID_RECOVERY_MIN_RADIUS,
                RAID_RECOVERY_MAX_RADIUS,
                12,
                true
        );
        if (recoveryPos == null) {
            return null;
        }

        var created = mob.getType().create(level);
        if (!(created instanceof Mob replacement)) {
            return null;
        }

        replacement.setPos(recoveryPos.getX() + 0.5D, recoveryPos.getY(), recoveryPos.getZ() + 0.5D);
        replacement.setPersistenceRequired();
        replacement.finalizeSpawn(level, level.getCurrentDifficultyAt(recoveryPos), MobSpawnType.EVENT, null);
        copyRaidState(mob, replacement);
        raid.applyRaidTags(replacement);
        replacement.setTarget(preferredTarget);
        replacement.setAggressive(true);

        if (!level.addFreshEntity(replacement)) {
            return null;
        }

        raid.trackReplacement(replacement);
        if (replacement instanceof PathfinderMob replacementPathfinder) {
            FactionSpawnHelper.moveToTargetWithPathFallback(replacementPathfinder, preferredTarget);
        }
        return replacement;
    }

    private static void copyRaidState(Mob original, Mob replacement) {
        replacement.setHealth(Math.min(replacement.getMaxHealth(), original.getHealth()));
        replacement.setCustomName(original.getCustomName());
        replacement.setCustomNameVisible(original.isCustomNameVisible());
        replacement.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, original.getMainHandItem().copy());
        replacement.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, original.getOffhandItem().copy());
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) {
                replacement.setItemSlot(slot, original.getItemBySlot(slot).copy());
            }
        }
        for (MobEffectInstance effect : original.getActiveEffects()) {
            replacement.addEffect(new MobEffectInstance(effect));
        }
        for (String tag : original.getTags()) {
            replacement.addTag(tag);
        }
    }

    @Nullable
    private static Player pickPreferredTarget(ServerLevel level, RaidContext raid) {
        ServerPlayer nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        double maxDistanceSq = (double) ACTIVE_RADIUS * (double) ACTIVE_RADIUS;
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (!isValidRaidTarget(player, level, raid.origin)) {
                continue;
            }

            double distanceSq = player.distanceToSqr(raid.origin.getX() + 0.5D, raid.origin.getY() + 0.5D, raid.origin.getZ() + 0.5D);
            if (distanceSq > maxDistanceSq || distanceSq >= nearestDistanceSq) {
                continue;
            }
            nearestDistanceSq = distanceSq;
            nearest = player;
        }
        raid.targetPlayerId = nearest != null ? nearest.getUUID() : raid.targetPlayerId;
        return nearest;
    }

    private static boolean isValidRaidTarget(@Nullable Player player, ServerLevel level, BlockPos origin) {
        if (player == null
                || player.level() != level
                || !player.isAlive()
                || player.isDeadOrDying()
                || player.isSpectator()) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) {
            return false;
        }
        return player.blockPosition().distSqr(origin) <= (double) ACTIVE_RADIUS * (double) ACTIVE_RADIUS;
    }

    private static void finishVictory(ServerLevel level, RaidContext raid) {
        raid.finished = true;
        raid.victory = true;
        raid.defeat = false;
        raid.spawningWave = false;
        raid.failedNoTargets = false;
        grantVictoryRewards(level, raid);
    }

    private static void grantVictoryRewards(ServerLevel level, RaidContext raid) {
        if (raid.rewardGranted) {
            return;
        }

        List<ServerPlayer> eligiblePlayers = new ArrayList<>();
        for (UUID playerId : raid.participantPlayerIds) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (!isValidRaidTarget(player, level, raid.origin)) {
                continue;
            }
            eligiblePlayers.add(player);
        }
        if (eligiblePlayers.isEmpty()) {
            return;
        }

        raid.rewardBarrelPositions.clear();
        raid.rewardBarrelPositions.add(placeRewardBarrel(level, findRewardTerrain(level, raid.origin)));
        if (eligiblePlayers.size() > 1) {
            raid.rewardBarrelPositions.add(placeRewardBarrel(level, findRewardTerrain(level, raid.origin.offset(2, 0, 2))));
        }
        raid.rewardGranted = true;
        raid.rewardMarkerActive = true;
        raid.rewardMarkerCooldown = REWARD_MARKER_TICK_INTERVAL;
    }

    private static BlockPos placeRewardBarrel(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
        LootUtils.fillContainer(level, pos, FACTION_RAID_REWARD_LOOT, level.getRandom());
        return pos.immutable();
    }

    private static BlockPos findRewardTerrain(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        while (cursor.getY() > level.getMinBuildHeight() && level.isEmptyBlock(cursor)) {
            cursor.move(0, -1, 0);
        }
        if (!level.getBlockState(cursor).isAir()) {
            cursor.move(0, 1, 0);
        }
        return cursor.immutable();
    }

    private static void tickRewardMarkers(ServerLevel level, RaidContext raid) {
        if (!raid.rewardMarkerActive || raid.rewardBarrelPositions.isEmpty()) {
            return;
        }
        for (BlockPos pos : raid.rewardBarrelPositions) {
            AABB checkZone = new AABB(pos).inflate(4.0D);
            boolean claimed = !level.getEntitiesOfClass(ServerPlayer.class, checkZone,
                    player -> isValidRaidTarget(player, level, raid.origin)).isEmpty();
            if (claimed) {
                raid.rewardMarkerActive = false;
                return;
            }
        }
        if (--raid.rewardMarkerCooldown > 0) {
            return;
        }
        raid.rewardMarkerCooldown = REWARD_MARKER_TICK_INTERVAL;
        for (BlockPos pos : raid.rewardBarrelPositions) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 1.15D;
            double z = pos.getZ() + 0.5D;
            level.sendParticles(ModParticleTypes.FLARE_SMOKE.get(), x, y, z, 2, 0.08D, 0.18D, 0.08D, 0.01D);
            level.sendParticles(ModParticleTypes.FLARE.get(), x, y + 0.1D, z, 1, 0.02D, 0.12D, 0.02D, 0.0D);
        }
    }

    private static void finishDefeat(ServerLevel level, RaidContext raid, boolean failedNoTargets) {
        clearActiveWaveMobs(level, raid);
        clearRemainingRaidMobs(level, raid);
        raid.finished = true;
        raid.defeat = true;
        raid.victory = false;
        raid.spawningWave = false;
        raid.failedNoTargets = failedNoTargets;
    }

    private static void clearActiveWaveMobs(ServerLevel level, RaidContext raid) {
        Set<UUID> mobIds = new HashSet<>(raid.activeMobIds);
        for (UUID mobId : mobIds) {
            Entity entity = level.getEntity(mobId);
            if (entity instanceof Mob mob && mob.isAlive() && !mob.isRemoved()) {
                mob.discard();
            }
            raid.clearMobTracking(mobId);
        }
        raid.activeMobIds.clear();
    }

    private static void clearRemainingRaidMobs(ServerLevel level, RaidContext raid) {
        AABB search = new AABB(raid.origin).inflate(LEGACY_RECOVERY_MERGE_DISTANCE * 4.0D);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, search, mob -> mob.getTags().contains(FactionSpawnHelper.RAID_TAG))) {
            UUID mobRaidId = parseUuid(readTagValue(mob, RAID_ID_TAG_PREFIX));
            if (!raid.raidId.equals(mobRaidId)) {
                continue;
            }
            if (mob.isAlive() && !mob.isRemoved()) {
                mob.discard();
            }
        }
    }

    @Nullable
    private static RaidEntity ensureAnchor(ServerLevel level, RaidContext raid, Vec3 pos) {
        RaidEntity anchor = findAnchor(level, raid);
        if (anchor != null) {
            return anchor;
        }

        anchor = new RaidEntity(ModEntities.RAID_ENTITY.get(), level);
        anchor.setPos(pos);
        anchor.bindToRaid(raid.raidId, raid.factionName, raid.forceGuns, raid.totalWaves, raid.currentWave);
        anchor.syncFromManager(
                raid.factionName,
                raid.currentWave,
                raid.totalWaves,
                raid.spawningWave,
                raid.spawnedThisWaveCount,
                raid.activeMobIds.size(),
                raid.waveCooldown,
                raid.finished,
                raid.victory,
                raid.defeat,
                raid.failedNoTargets,
                raid.rewardGranted
        );
        if (!level.addFreshEntity(anchor)) {
            return null;
        }
        raid.anchorId = anchor.getUUID();
        return anchor;
    }

    @Nullable
    private static RaidEntity findAnchor(ServerLevel level, RaidContext raid) {
        if (raid.anchorId != null) {
            Entity entity = level.getEntity(raid.anchorId);
            if (entity instanceof RaidEntity raidEntity && raidEntity.isAlive() && !raidEntity.isRemoved()) {
                return raidEntity;
            }
        }

        AABB search = new AABB(raid.origin).inflate(48.0D);
        List<RaidEntity> anchors = level.getEntitiesOfClass(RaidEntity.class, search,
                anchor -> raid.raidId.equals(anchor.getRaidId()) && anchor.isAlive() && !anchor.isRemoved());
        if (!anchors.isEmpty()) {
            RaidEntity anchor = anchors.get(0);
            raid.anchorId = anchor.getUUID();
            return anchor;
        }
        return null;
    }

    private static void syncAnchor(RaidEntity anchor, RaidContext raid) {
        anchor.bindToRaid(raid.raidId, raid.factionName, raid.forceGuns, raid.totalWaves, raid.currentWave);
        anchor.syncFromManager(
                raid.factionName,
                raid.currentWave,
                raid.totalWaves,
                raid.spawningWave,
                raid.spawnedThisWaveCount,
                raid.activeMobIds.size(),
                raid.waveCooldown,
                raid.finished,
                raid.victory,
                raid.defeat,
                raid.failedNoTargets,
                raid.rewardGranted
        );
    }

    private static void reconcileLoadedRaidMobs(ServerLevel level, RaidContext raid) {
        if (raid.finished) {
            return;
        }
        AABB search = new AABB(raid.origin).inflate(LEGACY_RECOVERY_MERGE_DISTANCE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, search, mob -> mob.getTags().contains(FactionSpawnHelper.RAID_TAG))) {
            UUID mobRaidId = parseUuid(readTagValue(mob, RAID_ID_TAG_PREFIX));
            if (mobRaidId != null && !raid.raidId.equals(mobRaidId)) {
                continue;
            }
            raid.trackRecoveredMob(mob);
        }
    }

    @Nullable
    private static Faction resolveFaction(String factionName) {
        GunnerManager manager = GunnerManager.getInstance();
        Faction faction = manager.getFactionByName(factionName);
        return faction != null ? faction : FactionSpawnHelper.getRandomFaction();
    }

    @Nullable
    private static RaidContext findContextById(List<RaidContext> raids, UUID raidId) {
        for (RaidContext raid : raids) {
            if (raid.raidId.equals(raidId)) {
                return raid;
            }
        }
        return null;
    }

    @Nullable
    private static RaidContext findCompatibleRecoveryContext(List<RaidContext> raids, BlockPos origin, @Nullable String factionName, @Nullable UUID targetPlayerId) {
        RaidContext best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (RaidContext raid : raids) {
            if (raid.finished) {
                continue;
            }
            if (factionName != null && !factionName.isBlank() && !raid.factionName.equals(factionName)) {
                continue;
            }
            if (targetPlayerId != null && raid.targetPlayerId != null && !targetPlayerId.equals(raid.targetPlayerId)) {
                continue;
            }

            int distance = raid.origin.distManhattan(origin);
            if (distance > LEGACY_RECOVERY_MERGE_DISTANCE || distance >= bestDistance) {
                continue;
            }
            best = raid;
            bestDistance = distance;
        }
        return best;
    }

    @Nullable
    private static RaidContext findCompatibleLegacyContext(List<RaidContext> raids, BlockPos origin, String factionName) {
        return findCompatibleRecoveryContext(raids, origin, factionName, null);
    }

    @Nullable
    private static String readTagValue(Mob mob, String prefix) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
    }

    @Nullable
    private static UUID parseUuid(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void replaceTagValue(Mob mob, String prefix, @Nullable String value) {
        List<String> stale = new ArrayList<>();
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                stale.add(tag);
            }
        }
        for (String tag : stale) {
            mob.removeTag(tag);
        }
        if (value != null && !value.isBlank()) {
            mob.addTag(prefix + value);
        }
    }

    private static void playHorn(ServerLevel level, BlockPos origin, boolean celebration) {
        int x = level.getRandom().nextInt(-25, 25);
        int z = level.getRandom().nextInt(-25, 25);
        int soundIndex = celebration ? (level.getRandom().nextBoolean() ? 1 : 2) : 0;
        level.playSound(null, BlockPos.containing(Vec3.atCenterOf(origin).add(x, 24, z)), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(soundIndex).value(), SoundSource.HOSTILE, 4.0F, 1.0F);
    }

    private static final class RaidContext {
        private final UUID raidId;
        private final BlockPos origin;
        private final String factionName;
        private final boolean forceGuns;
        private final int totalWaves;
        private final Set<UUID> activeMobIds = new HashSet<>();
        private final Set<UUID> participantPlayerIds = new HashSet<>();
        private final List<BlockPos> rewardBarrelPositions = new ArrayList<>();
        private final Map<UUID, Vec3> lastPositions = new HashMap<>();
        private final Map<UUID, Integer> stationaryTicks = new HashMap<>();
        private final Map<UUID, Float> lastYaws = new HashMap<>();
        private final Map<UUID, Integer> spinTicks = new HashMap<>();
        private final Map<UUID, Double> lastTargetDistanceSq = new HashMap<>();
        private final Map<UUID, Integer> distanceStallTicks = new HashMap<>();
        private @Nullable UUID targetPlayerId;
        private @Nullable UUID anchorId;
        private @Nullable BlockPos currentBurstCenter;
        private int currentWave;
        private int waveCooldown = 20;
        private int spawnCooldown = SPAWN_INTERVAL_TICKS;
        private int spawnedThisWaveCount;
        private int spawnedInCurrentBurst;
        private int reconcileCooldown = 1;
        private int lowMobNoFireTicks;
        private int rewardMarkerCooldown = REWARD_MARKER_TICK_INTERVAL;
        private boolean hadPlayersInRange;
        private boolean failedNoTargets;
        private boolean spawningWave;
        private boolean finished;
        private boolean victory;
        private boolean defeat;
        private boolean rewardGranted;
        private boolean rewardMarkerActive;

        private RaidContext(UUID raidId, BlockPos origin, String factionName, boolean forceGuns, int totalWaves) {
            this.raidId = raidId;
            this.origin = origin.immutable();
            this.factionName = factionName;
            this.forceGuns = forceGuns;
            this.totalWaves = Math.max(1, totalWaves);
        }

        private void trackSpawn(Mob mob, @Nullable Player preferredTarget) {
            this.activeMobIds.add(mob.getUUID());
            this.spawnedThisWaveCount++;
            if (preferredTarget != null) {
                this.targetPlayerId = preferredTarget.getUUID();
                this.participantPlayerIds.add(preferredTarget.getUUID());
            }
            clearMobTracking(mob.getUUID());
            applyRaidTags(mob);
        }

        private void trackReplacement(Mob mob) {
            this.activeMobIds.add(mob.getUUID());
            clearMobTracking(mob.getUUID());
            applyRaidTags(mob);
        }

        private void trackRecoveredMob(Mob mob) {
            if (this.activeMobIds.add(mob.getUUID())) {
                this.spawnedThisWaveCount = Math.max(this.spawnedThisWaveCount, this.activeMobIds.size());
            }
            clearMobTracking(mob.getUUID());
            applyRaidTags(mob);
        }

        private void applyRaidTags(Mob mob) {
            mob.addTag(FactionSpawnHelper.RAID_TAG);
            replaceTagValue(mob, RAID_ID_TAG_PREFIX, this.raidId.toString());
            replaceTagValue(mob, RAID_ORIGIN_TAG_PREFIX, this.origin.getX() + "," + this.origin.getY() + "," + this.origin.getZ());
            replaceTagValue(mob, RAID_FACTION_TAG_PREFIX, this.factionName);
            replaceTagValue(mob, RAID_TOTAL_WAVES_TAG_PREFIX, Integer.toString(this.totalWaves));
            replaceTagValue(mob, RAID_CURRENT_WAVE_TAG_PREFIX, Integer.toString(Math.max(1, this.currentWave)));
            replaceTagValue(mob, RAID_TARGET_TAG_PREFIX, this.targetPlayerId != null ? this.targetPlayerId.toString() : null);
            replaceTagValue(mob, RAID_FORCE_GUNS_TAG_PREFIX, Boolean.toString(this.forceGuns));
        }

        private void refreshActiveMobs(ServerLevel level) {
            Set<UUID> removed = new HashSet<>();
            this.activeMobIds.removeIf(uuid -> {
                Entity entity = level.getEntity(uuid);
                boolean missing = !(entity instanceof Mob mob) || mob.isRemoved() || !mob.isAlive();
                if (missing) {
                    removed.add(uuid);
                }
                return missing;
            });
            for (UUID mobId : removed) {
                clearMobTracking(mobId);
            }
        }

        private int burstSize(RandomSource random) {
            return random.nextInt(RAID_BURST_SIZE_MAX - RAID_BURST_SIZE_MIN + 1) + RAID_BURST_SIZE_MIN;
        }

        private void clearMobTracking(UUID mobId) {
            this.lastPositions.remove(mobId);
            this.stationaryTicks.remove(mobId);
            this.lastYaws.remove(mobId);
            this.spinTicks.remove(mobId);
            this.lastTargetDistanceSq.remove(mobId);
            this.distanceStallTicks.remove(mobId);
        }
    }
}
