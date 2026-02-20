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
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.init.ModEffects;

public final class PatrolEncounterManager {
    private static final Map<ServerLevel, List<PatrolContext>> ACTIVE_PATROLS = new HashMap<>();
    private static final int OMEN_DURATION_TICKS = 36000;
    private static final int LEGACY_RECOVERY_MERGE_DISTANCE = 128;
    private static final int UNREACHABLE_REPATH_TICKS = 80;
    private static final int UNREACHABLE_RELOCATE_TICKS = 200;
    private static final int UNREACHABLE_CLEANUP_TICKS = 500;
    private static final int PATROL_TARGET_RANGE = 96;
    private static final double PATROL_NAVIGATION_SPEED = 1.2D;

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
            context.mobIds.add(mob.getUUID());
            replaceTagValue(mob, FactionSpawnHelper.PATROL_ID_TAG_PREFIX, context.patrolId);
            replaceTagValue(mob, FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX, context.factionName);
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
        if (factionName == null || factionName.isBlank()) {
            factionName = "night_of_the_undead";
        }

        List<PatrolContext> contexts = ACTIVE_PATROLS.computeIfAbsent(level, ignored -> new ArrayList<>());
        PatrolContext context = !missingPatrolId ? findContextById(contexts, patrolId) : null;
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

        if (context.mobIds.add(mob.getUUID())) {
            context.initialCount = Math.max(context.initialCount, context.mobIds.size());
        }

        replaceTagValue(mob, FactionSpawnHelper.PATROL_ID_TAG_PREFIX, context.patrolId);
        replaceTagValue(mob, FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX, context.factionName);
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

        for (UUID mobId : context.mobIds) {
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
        if (alive > 0) {
            return false;
        }

        awardOmen(level, context);
        context.bossBar.removeAllPlayers();
        return true;
    }

    private static boolean maintainPatrolMob(ServerLevel level, PatrolContext context, UUID mobId, Mob mob, @Nullable ServerPlayer preferredTarget) {
        if (!isValidPatrolTarget(level, preferredTarget)) {
            return false;
        }

        LivingEntity currentTarget = mob.getTarget();
        if (!(currentTarget instanceof ServerPlayer currentPlayer) || !isValidPatrolTarget(level, currentPlayer)) {
            mob.setTarget(preferredTarget);
            mob.setAggressive(true);
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            context.clearMobTracking(mobId);
            return false;
        }

        double distanceSq = mob.distanceToSqr(preferredTarget);
        boolean farFromTarget = distanceSq > 24.0D * 24.0D;
        boolean navDone = pathfinderMob.getNavigation().isDone();
        boolean noLineOfSight = !mob.hasLineOfSight(preferredTarget);
        boolean stuck = farFromTarget && (navDone || noLineOfSight);

        if (!stuck) {
            context.clearMobTracking(mobId);
            return false;
        }

        int stuckTicks = context.incrementUnreachableTicks(mobId);
        if (stuckTicks % UNREACHABLE_REPATH_TICKS == 0) {
            forceRepath(pathfinderMob, preferredTarget);
        }
        if (stuckTicks % UNREACHABLE_RELOCATE_TICKS == 0) {
            relocateNearTarget(level, pathfinderMob, preferredTarget);
            context.incrementRelocationCount(mobId);
        }

        if (stuckTicks >= UNREACHABLE_CLEANUP_TICKS || context.getRelocationCount(mobId) >= 3) {
            mob.discard();
            context.clearMobTracking(mobId);
            return true;
        }
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

    private static void relocateNearTarget(ServerLevel level, PathfinderMob mob, ServerPlayer target) {
        BlockPos relocatePos = FactionSpawnHelper.sampleGroundPosition(level, target.blockPosition(), level.getRandom(), 4, 10);
        if (!FactionSpawnHelper.isSafeGroundPosition(level, relocatePos)) {
            return;
        }
        mob.teleportTo(relocatePos.getX() + 0.5D, relocatePos.getY(), relocatePos.getZ() + 0.5D);
        mob.getNavigation().stop();
        mob.setTarget(target);
        mob.setAggressive(true);
        forceRepath(mob, target);
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
        player.addEffect(new MobEffectInstance(ModEffects.FACTION_OMEN, OMEN_DURATION_TICKS, 0, false, true));
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
        private int initialCount;
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
        }
    }
}
