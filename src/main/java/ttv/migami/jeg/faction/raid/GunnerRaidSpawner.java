package ttv.migami.jeg.faction.raid;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;

public final class GunnerRaidSpawner {
    private int nextTick;
    private boolean initialized;

    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        if (!Config.factionRaidEnabled()) {
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
        if (day < Config.factionRaidMinimumDays()) {
            return 0;
        }

        Player randomPlayer = getRandomEligiblePlayer(level, random);
        if (randomPlayer == null) {
            return 0;
        }

        Faction faction = FactionSpawnHelper.getRandomFaction();
        if (faction == null) {
            return 0;
        }

        FactionRaidManager.startRaid(level, faction, randomPlayer.position(), true);
        return 1;
    }

    private void scheduleNextTick(RandomSource random) {
        int intervalDays = Config.factionRaidIntervalDays();
        if (intervalDays > 0) {
            this.nextTick = intervalDays * 24000;
        } else {
            int min = Config.factionRaidRandomIntervalMinTicks();
            int max = Config.factionRaidRandomIntervalMaxTicks();
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
