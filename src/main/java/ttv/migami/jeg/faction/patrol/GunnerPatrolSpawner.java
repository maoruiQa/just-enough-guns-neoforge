package ttv.migami.jeg.faction.patrol;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;

public final class GunnerPatrolSpawner {
    private int nextTick;
    private boolean initialized;

    public void reschedule() {
        this.initialized = false;
        this.nextTick = 0;
    }

    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        if (!Config.factionPatrolEnabled()) {
            return 0;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0;
        }
        if (!spawnEnemies || !Boolean.TRUE.equals(level.getGameRules().get(GameRules.SPAWN_PATROLS))) {
            return 0;
        }

        RandomSource random = level.getRandom();
        if (!this.initialized) {
            scheduleNextTick(random);
            this.initialized = true;
            return 0;
        }

        this.nextTick--;
        if (this.nextTick > 0) {
            return 0;
        }

        scheduleNextTick(random);

        long day = Config.currentGunnerDay(level);
        if (day < Config.factionPatrolMinimumDays()) {
            return 0;
        }

        Player randomPlayer = getRandomEligiblePlayer(level, random);
        if (randomPlayer == null) {
            return 0;
        }
        if (random.nextDouble() >= Config.factionPatrolSpawnChance()) {
            return 0;
        }

        BlockPos.MutableBlockPos spawnPos = randomPlayer.blockPosition().mutable()
                .move((24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1), 0,
                        (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1));

        if (!level.hasChunksAt(spawnPos.getX() - 10, spawnPos.getZ() - 10, spawnPos.getX() + 10, spawnPos.getZ() + 10)) {
            return 0;
        }

        Faction faction = FactionSpawnHelper.getRandomFaction();
        if (faction == null) {
            return 0;
        }

        int patrolCount = 2 + random.nextInt(5);
        List<Mob> spawned = FactionSpawnHelper.spawnPatrol(level, faction, patrolCount, randomPlayer, spawnPos);
        if (spawned.isEmpty()) {
            return 0;
        }

        PatrolEncounterManager.startEncounter(level, faction, spawnPos.immutable(), spawned);
        return spawned.size();
    }

    private void scheduleNextTick(RandomSource random) {
        int intervalDays = Config.factionPatrolIntervalDays();
        if (intervalDays > 0) {
            this.nextTick = intervalDays * 24000;
        } else {
            int min = Config.factionPatrolRandomIntervalMinTicks();
            int max = Config.factionPatrolRandomIntervalMaxTicks();
            this.nextTick = min + random.nextInt(max - min + 1);
        }
        this.nextTick += random.nextInt(12000);
    }

    private static Player getRandomEligiblePlayer(ServerLevel level, RandomSource random) {
        List<ServerPlayer> players = level.getPlayers(player -> isEligibleTarget(level, player));
        return players.isEmpty() ? null : players.get(random.nextInt(players.size()));
    }

    private static boolean isEligibleTarget(ServerLevel level, ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                && !level.isCloseToVillage(player.blockPosition(), 2);
    }
}
