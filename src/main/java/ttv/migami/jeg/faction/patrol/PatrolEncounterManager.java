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
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.init.ModEffects;

public final class PatrolEncounterManager {
    private static final Map<ServerLevel, List<PatrolContext>> ACTIVE_PATROLS = new HashMap<>();
    private static final int OMEN_DURATION_TICKS = 36000;

    private PatrolEncounterManager() {}

    public static void startEncounter(ServerLevel level, Faction faction, BlockPos origin, List<Mob> mobs) {
        if (mobs.isEmpty()) {
            return;
        }

        String patrolId = readTagValue(mobs.get(0), FactionSpawnHelper.PATROL_ID_TAG_PREFIX);
        if (patrolId == null || patrolId.isBlank()) {
            patrolId = UUID.randomUUID().toString();
        }

        PatrolContext context = new PatrolContext(faction.getName(), origin, mobs.size(), patrolId);
        for (Mob mob : mobs) {
            context.mobIds.add(mob.getUUID());
        }

        ACTIVE_PATROLS.computeIfAbsent(level, ignored -> new ArrayList<>()).add(context);
    }

    public static void recoverPatrolMob(ServerLevel level, Mob mob) {
        if (!mob.getTags().contains(FactionSpawnHelper.PATROL_TAG)) {
            return;
        }

        String patrolId = readTagValue(mob, FactionSpawnHelper.PATROL_ID_TAG_PREFIX);
        if (patrolId == null || patrolId.isBlank()) {
            patrolId = "legacy-" + mob.getUUID();
        }

        String factionName = readTagValue(mob, FactionSpawnHelper.PATROL_FACTION_TAG_PREFIX);
        if (factionName == null || factionName.isBlank()) {
            factionName = "night_of_the_undead";
        }

        List<PatrolContext> contexts = ACTIVE_PATROLS.computeIfAbsent(level, ignored -> new ArrayList<>());
        PatrolContext context = findContextById(contexts, patrolId);
        if (context == null) {
            context = new PatrolContext(factionName, mob.blockPosition(), 0, patrolId);
            contexts.add(context);
        }

        if (context.mobIds.add(mob.getUUID())) {
            context.initialCount = Math.max(context.initialCount, context.mobIds.size());
        }
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
    private static String readTagValue(Mob mob, String prefix) {
        for (String tag : mob.getTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return null;
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

        for (UUID mobId : context.mobIds) {
            Entity entity = level.getEntity(mobId);
            if (!(entity instanceof LivingEntity living)) {
                toRemove.add(mobId);
                continue;
            }

            if (living.isAlive() && !living.isRemoved()) {
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

        player.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.FACTION_OMEN.get()),
                OMEN_DURATION_TICKS,
                0,
                false,
                true
        ));
        player.sendSystemMessage(Component.translatable("message.jeg.faction_patrol.omen"));
    }

    private static final class PatrolContext {
        private final String patrolId;
        private final String factionName;
        private final BlockPos origin;
        private final Set<UUID> mobIds = new HashSet<>();
        private final ServerBossEvent bossBar;
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
    }
}
