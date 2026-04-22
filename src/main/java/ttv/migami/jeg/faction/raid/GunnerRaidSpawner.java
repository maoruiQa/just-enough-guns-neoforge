package ttv.migami.jeg.faction.raid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;

public final class GunnerRaidSpawner {
    private int nextTick;

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
        this.nextTick--;
        if (this.nextTick > 0) {
            return 0;
        }

        int intervalDays = Config.factionRaidIntervalDays();
        if (intervalDays > 0) {
            this.nextTick += intervalDays * 24000;
        } else {
            int min = Config.factionRaidRandomIntervalMinTicks();
            int max = Config.factionRaidRandomIntervalMaxTicks();
            this.nextTick += min + random.nextInt(max - min + 1);
        }
        this.nextTick += random.nextInt(12000);

        long day = level.getOverworldClockTime() / 24000L;
        if (day < Config.factionRaidMinimumDays()) {
            return 0;
        }
        if (level.players().isEmpty()) {
            return 0;
        }

        Player randomPlayer = level.players().get(random.nextInt(level.players().size()));
        if (randomPlayer.isSpectator() || level.isCloseToVillage(randomPlayer.blockPosition(), 2)) {
            return 0;
        }

        Faction faction = FactionSpawnHelper.getRandomFaction();
        if (faction == null) {
            return 0;
        }

        FactionRaidManager.startRaid(level, faction, randomPlayer.position(), true);
        return 1;
    }
}
