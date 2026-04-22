package ttv.migami.jeg.faction.patrol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GroupedGunnerRecovery;
import ttv.migami.jeg.init.ModEffects;

public final class PatrolEncounterManager {
    private static final Map<ServerLevel, List<PatrolContext>> ACTIVE_PATROLS = new HashMap<>();
    private static final int OMEN_DURATION_TICKS = 36000;
    private static final int LEGACY_RECOVERY_MERGE_DISTANCE = 128;
    private static final int UNREACHABLE_REPATH_TICKS = 3;
    private static final int UNREACHABLE_RELOCATE_TICKS = 8;
    private static final int UNREACHABLE_CLEANUP_TICKS = 24;
    private static final int PATROL_NO_PLAYER_TIMEOUT_TICKS = 1200;
    private static final int PATROL_TARGET_RANGE = 96;
    private static final double PATROL_NAVIGATION_SPEED = 1.2D;
    private static final int STATIONARY_STUCK_TICKS = 120;
    private static final int SPIN_STUCK_TICKS = 120;
    private static final int DISTANCE_STALL_TICKS = 120;
    private static final double STATIONARY_MOVEMENT_THRESHOLD_SQ = 0.25D;
    private static final float SPIN_STUCK_YAW_DELTA_DEGREES = 35.0F;
    private static final double DISTANCE_STALL_THRESHOLD = 1.5D;
    private static final int PATROL_RELOCATE_MIN_DISTANCE = 12;
    private static final int PATROL_RELOCATE_MAX_DISTANCE = 28;

    private PatrolEncounterManager() {}

    public static void startEncounter(ServerLevel level, Faction faction, BlockPos origin, List<Mob> mobs) {
        if (mobs.isEmpty()) {
            return;
        }

        String patrolId = readTagValue(mobs.get(0), FactionSpawnHelper.PATROL_ID_TAG_PREFIX);
        if (patrolId == null || patrolId.isBlank()) {
            patrolId = UUID.randomUUID().toString();
        }

        List<PatrolContext> contexts = ACTIVE_PATROLS.computeIfAbsent(level, ignored -> new ArrayList<>());
        PatrolContext context = findContextById(contexts, patrolId);
        if (context == null) {
            context = new PatrolContext(faction.getName(), origin, mobs.size(), patrolId);
            contexts.add(context);
        }
        for (Mob mob : mobs) {
            context.trackMob(mob);
        }
        context.initialCount = Math.max(context.initialCount, context.mobIds.size());
    }

    public static void recoverPatrolMob(ServerLevel level, Mob mob) {
        if (!mob.getTags().contains(FactionSpawnHelper.PATROL_TAG)) {
            return;
        }

        String patrolId = readTagValue(mob, FactionSpawnHelper.PATROL_ID_TAG_PREFIX);
        boolean missingPatrolId = patrolId == null || patrolId.isBlank();
        String factionName = readTagValue(mob, FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX);

        List<PatrolContext> contexts = ACTIVE_PATROLS.computeIfAbsent(level, ignored -> new ArrayList<>());
        PatrolContext context = !missingPatrolId ? findContextById(contexts, patrolId) : null;
        if ((factionName == null || factionName.isBlank()) && context != null) {
            factionName = context.factionName;
        }
        if (factionName == null || factionName.isBlank()) {
            return;
        }
        if (context == null) {
            context = findCompatibleLegacyContext(contexts, factionName, mob.blockPosition());
        }
        if (context == null) {
            if (missingPatrolId) {
                patrolId = "legacy-" + UUID.randomUUID();
            }
            context = new PatrolContext(factionName, mob.blockPosition(), 0, patrolId);
            contexts.add(context);
        }

        context.trackMob(mob);
        context.initialCount = Math.max(context.initialCount, context.mobIds.size());
    }

    @Nullable
    private static PatrolContext findContextById(List<PatrolContext> contexts, String patrolId) {
        for (PatrolContext context : contexts) {
            if (context.patrolId.equals(patrolId)) {
                return context;
            }
        }
        return null;
    }

    @Nullable
    private static PatrolContext findCompatibleLegacyContext(List<PatrolContext> contexts, String factionName, BlockPos origin) {
        PatrolContext best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (PatrolContext context : contexts) {
            if (!context.factionName.equals(factionName)) {
                continue;
            }

            int distance = context.origin.distManhattan(origin);
            if (distance > LEGACY_RECOVERY_MERGE_DISTANCE || distance >= bestDistance) {
                continue;
            }

            best = context;
            bestDistance = distance;
        }
        return best;
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

    public static void tickAll(MinecraftServer server) {
        Iterator<Map.Entry<ServerLevel, List<PatrolContext>>> levelIterator = ACTIVE_PATROLS.entrySet().iterator();
        while (levelIterator.hasNext()) {
            Map.Entry<ServerLevel, List<PatrolContext>> levelEntry = levelIterator.next();
            ServerLevel level = levelEntry.getKey();
            if (level.getServer() != server) {
                continue;
            }

            List<PatrolContext> contexts = levelEntry.getValue();
            Iterator<PatrolContext> iterator = contexts.iterator();
            while (iterator.hasNext()) {
                PatrolContext context = iterator.next();
                if (tickContext(level, context)) {
                    iterator.remove();
                }
            }

            if (contexts.isEmpty()) {
                levelIterator.remove();
            }
        }
    }

    private static boolean tickContext(ServerLevel level, PatrolContext context) {
        int alive = 0;
        UUID killerCandidate = null;
        boolean hadDeath = false;
        Set<UUID> toRemove = new HashSet<>();
        ServerPlayer preferredTarget = resolvePreferredTarget(level, context);
        boolean hasPreferredTarget = isValidPatrolTarget(level, preferredTarget);

        if (hasPreferredTarget) {
            context.noPlayerTicks = 0;
        } else {
            context.noPlayerTicks++;
        }

        for (UUID mobId : new HashSet<>(context.mobIds)) {
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof LivingEntity living)) {
                toRemove.add(mobId);
                continue;
            }

            if (living.isAlive() && !living.isRemoved()) {
                if (living instanceof Mob mob && maintainPatrolMob(level, context, mobId, mob, preferredTarget)) {
                    toRemove.add(mobId);
                    continue;
                }
                alive++;
                continue;
            }

            hadDeath = true;
            if (living.getKillCredit() instanceof ServerPlayer killer) {
                killerCandidate = killer.getUUID();
            } else {
                killerCandidate = null;
            }
            toRemove.add(mobId);
        }

        context.mobIds.removeAll(toRemove);
        for (UUID mobId : toRemove) {
            context.clearMobTracking(mobId);
        }
        if (hadDeath) {
            context.lastKiller = killerCandidate;
        }

        float progress = context.initialCount <= 0 ? 0.0F : (float) alive / (float) context.initialCount;
        context.bossBar.setProgress(Mth.clamp(progress, 0.0F, 1.0F));
        context.bossBar.setName(Component.translatable(
                "message.jeg.faction_patrol.bossbar",
                Component.translatable("faction.jeg." + context.factionName),
                alive
        ));

        refreshPlayers(level, context);
        if (alive > 0 && (hasPreferredTarget || context.noPlayerTicks < PATROL_NO_PLAYER_TIMEOUT_TICKS)) {
            return false;
        }

        awardOmen(level, context);
        clearTrackedMobs(level, context);
        context.bossBar.removeAllPlayers();
        return true;
    }

    private static boolean maintainPatrolMob(ServerLevel level, PatrolContext context, UUID mobId, Mob mob, @Nullable ServerPlayer preferredTarget) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            context.clearMobTracking(mobId);
            return false;
        }

        if (!mob.getTags().contains(FactionSpawnHelper.PATROL_TAG)) {
            context.clearMobTracking(mobId);
            return true;
        }

        if (!isValidPatrolTarget(level, preferredTarget)) {
            context.clearMobTracking(mobId);
            return false;
        }

        LivingEntity currentTarget = mob.getTarget();
        if (!(currentTarget instanceof ServerPlayer currentPlayer) || !isValidPatrolTarget(level, currentPlayer)) {
            mob.setTarget(preferredTarget);
            mob.setAggressive(true);
            forceRepath(pathfinderMob, preferredTarget);
        }

        int stationaryTicks = updateStationaryTicks(context, mobId, mob);
        int spinTicks = updateSpinTicks(context, mobId, mob);
        int distanceStallTicks = updateDistanceStallTicks(context, mobId, mob, preferredTarget);
        int stuckSeverity = Math.max(stationaryTicks, Math.max(spinTicks, distanceStallTicks));
        if (stuckSeverity < 60) {
            return false;
        }
        if (stuckSeverity < STATIONARY_STUCK_TICKS) {
            FactionSpawnHelper.trySoftTargetRecovery(pathfinderMob, preferredTarget);
            return false;
        }

        int eligibleCount = Math.min(3, countEligibleRecoveryMobs(context, level, preferredTarget));
        if (GroupedGunnerRecovery.tryRecoverGroundMob(
                level,
                "patrol:" + context.patrolId,
                context.origin,
                pathfinderMob,
                preferredTarget,
                PATROL_RELOCATE_MIN_DISTANCE,
                Math.min(PATROL_RELOCATE_MAX_DISTANCE, 12),
                PATROL_RELOCATE_MIN_DISTANCE,
                PATROL_RELOCATE_MAX_DISTANCE,
                12,
                eligibleCount
        )) {
            context.incrementRelocationCount(mobId);
            context.clearMobTracking(mobId);
            return false;
        }

        FactionSpawnHelper.trySoftTargetRecovery(pathfinderMob, preferredTarget);
        return false;
    }

    private static @Nullable ServerPlayer resolvePreferredTarget(ServerLevel level, PatrolContext context) {
        ServerPlayer nearest = null;
        double nearestDistanceSq = Double.MAX_VALUE;
        double cx = context.origin.getX() + 0.5D;
        double cy = context.origin.getY() + 0.5D;
        double cz = context.origin.getZ() + 0.5D;
        double maxDistanceSq = (double) PATROL_TARGET_RANGE * (double) PATROL_TARGET_RANGE;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (!isValidPatrolTarget(level, player)) {
                continue;
            }
            double distanceSq = player.distanceToSqr(cx, cy, cz);
            if (distanceSq > maxDistanceSq || distanceSq >= nearestDistanceSq) {
                continue;
            }
            nearest = player;
            nearestDistanceSq = distanceSq;
            context.targetPlayerId = player.getUUID();
        }
        return nearest;
    }

    private static boolean isValidPatrolTarget(ServerLevel level, @Nullable ServerPlayer player) {
        return player != null
                && player.level() == level
                && player.isAlive()
                && !player.isDeadOrDying()
                && !player.isSpectator()
                && !player.isCreative();
    }

    private static void forceRepath(PathfinderMob mob, ServerPlayer target) {
        if (mob.getNavigation().moveTo(target, PATROL_NAVIGATION_SPEED)) {
            return;
        }
        var path = mob.getNavigation().createPath(target, 0);
        if (path != null) {
            mob.getNavigation().moveTo(path, PATROL_NAVIGATION_SPEED);
        }
    }

    private static int updateStationaryTicks(PatrolContext context, UUID mobId, Mob mob) {
        return FactionSpawnHelper.updateStationaryTicks(
                context.lastPositions,
                context.stationaryTicks,
                mobId,
                mob.position(),
                20,
                STATIONARY_MOVEMENT_THRESHOLD_SQ
        );
    }

    private static int updateSpinTicks(PatrolContext context, UUID mobId, Mob mob) {
        float currentYaw = mob.getYRot();
        Float lastYaw = context.lastYaws.put(mobId, currentYaw);
        if (lastYaw == null) {
            context.spinTicks.remove(mobId);
            return 0;
        }
        float delta = Math.abs(Mth.wrapDegrees(currentYaw - lastYaw));
        if (delta < SPIN_STUCK_YAW_DELTA_DEGREES) {
            context.spinTicks.remove(mobId);
            return 0;
        }
        return context.spinTicks.merge(mobId, 20, Integer::sum);
    }

    private static int updateDistanceStallTicks(PatrolContext context, UUID mobId, Mob mob, ServerPlayer preferredTarget) {
        double distanceSq = mob.distanceToSqr(preferredTarget);
        Double lastDistanceSq = context.lastTargetDistanceSq.put(mobId, distanceSq);
        if (lastDistanceSq == null || lastDistanceSq - distanceSq > DISTANCE_STALL_THRESHOLD) {
            context.distanceStallTicks.remove(mobId);
            return 0;
        }
        return context.distanceStallTicks.merge(mobId, 20, Integer::sum);
    }

    private static int countEligibleRecoveryMobs(PatrolContext context, ServerLevel level, ServerPlayer preferredTarget) {
        int eligible = 0;
        for (UUID id : context.mobIds) {
            Entity entity = level.getEntity(id);
            if (!(entity instanceof Mob mob) || !(mob instanceof PathfinderMob)) {
                continue;
            }
            if (preferredTarget.hasLineOfSight(mob)) {
                continue;
            }
            int severity = Math.max(
                    context.stationaryTicks.getOrDefault(id, 0),
                    Math.max(context.spinTicks.getOrDefault(id, 0), context.distanceStallTicks.getOrDefault(id, 0))
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

    private static void clearTrackedMobs(ServerLevel level, PatrolContext context) {
        for (UUID mobId : new HashSet<>(context.mobIds)) {
            Entity entity = level.getEntity(mobId);
            if (entity instanceof Mob mob && mob.isAlive() && !mob.isRemoved()) {
                mob.discard();
            }
            context.clearMobTracking(mobId);
        }
        context.mobIds.clear();
    }

    private static void refreshPlayers(ServerLevel level, PatrolContext context) {
        int range = Config.factionPatrolBossBarRange();
        double radiusSq = (double) range * (double) range;
        double cx = context.origin.getX() + 0.5D;
        double cy = context.origin.getY() + 0.5D;
        double cz = context.origin.getZ() + 0.5D;

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            boolean inRange = player.level() == level
                    && !player.isSpectator()
                    && player.distanceToSqr(cx, cy, cz) <= radiusSq;
            if (inRange) {
                context.bossBar.addPlayer(player);
            } else {
                context.bossBar.removePlayer(player);
            }
        }
    }

    private static void awardOmen(ServerLevel level, PatrolContext context) {
        if (context.lastKiller == null) {
            return;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(context.lastKiller);
        if (player == null) {
            return;
        }

        setOmenFactionTag(player, context.factionName);
        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.FACTION_OMEN.get()),
                OMEN_DURATION_TICKS,
                0,
                false,
                true
        ));
        player.sendSystemMessage(Component.translatable("message.jeg.faction_patrol.omen"));
    }

    private static void setOmenFactionTag(ServerPlayer player, String factionName) {
        List<String> stale = new ArrayList<>();
        for (String tag : player.getTags()) {
            if (tag.startsWith(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX)) {
                stale.add(tag);
            }
        }
        for (String tag : stale) {
            player.removeTag(tag);
        }
        if (!factionName.isBlank()) {
            player.addTag(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX + factionName);
        }
    }

    private static final class PatrolContext {
        private final String patrolId;
        private final String factionName;
        private final BlockPos origin;
        private final Set<UUID> mobIds = new HashSet<>();
        private final ServerBossEvent bossBar;
        private final Map<UUID, Integer> unreachableTicks = new HashMap<>();
        private final Map<UUID, Integer> relocationCounts = new HashMap<>();
        private final Map<UUID, Vec3> lastPositions = new HashMap<>();
        private final Map<UUID, Integer> stationaryTicks = new HashMap<>();
        private final Map<UUID, Float> lastYaws = new HashMap<>();
        private final Map<UUID, Integer> spinTicks = new HashMap<>();
        private final Map<UUID, Double> lastTargetDistanceSq = new HashMap<>();
        private final Map<UUID, Integer> distanceStallTicks = new HashMap<>();
        private @Nullable UUID targetPlayerId;
        private int initialCount;
        private int noPlayerTicks;
        private UUID lastKiller;

        private PatrolContext(String factionName, BlockPos origin, int initialCount, String patrolId) {
            this.patrolId = patrolId;
            this.factionName = factionName;
            this.origin = origin.immutable();
            this.initialCount = initialCount;
            this.bossBar = new ServerBossEvent(
                    Component.translatable("message.jeg.faction_patrol.bossbar",
                            Component.translatable("faction.jeg." + factionName),
                            initialCount),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS
            );
        }

        private void trackMob(Mob mob) {
            this.mobIds.add(mob.getUUID());
            clearMobTracking(mob.getUUID());
            applyPatrolTags(mob);
        }

        private void applyPatrolTags(Mob mob) {
            mob.addTag(FactionSpawnHelper.PATROL_TAG);
            replaceTagValue(mob, FactionSpawnHelper.PATROL_ID_TAG_PREFIX, this.patrolId);
            replaceTagValue(mob, FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX, this.factionName);
        }

        private int incrementUnreachableTicks(UUID mobId) {
            return this.unreachableTicks.merge(mobId, 1, Integer::sum);
        }

        private void incrementRelocationCount(UUID mobId) {
            this.relocationCounts.merge(mobId, 1, Integer::sum);
        }

        private int getRelocationCount(UUID mobId) {
            return this.relocationCounts.getOrDefault(mobId, 0);
        }

        private void clearMobTracking(UUID mobId) {
            this.unreachableTicks.remove(mobId);
            this.relocationCounts.remove(mobId);
            this.lastPositions.remove(mobId);
            this.stationaryTicks.remove(mobId);
            this.lastYaws.remove(mobId);
            this.spinTicks.remove(mobId);
            this.lastTargetDistanceSq.remove(mobId);
            this.distanceStallTicks.remove(mobId);
        }
    }
}
