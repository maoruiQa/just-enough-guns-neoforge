package ttv.migami.jeg.faction.raid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;

public final class GunnerRaidSpawner {
    private int nextTick;

    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        if (!Config.factionRaidEnabled()) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: config disabled");
            return 0;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: peaceful difficulty");
            return 0;
        }
        if (!spawnEnemies || !level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)) {
            JustEnoughGuns.LOGGER.debug(
                    "[FactionRaid] Skip natural raid tick: spawnEnemies={} doPatrolSpawning={}",
                    spawnEnemies, level.getGameRules().getBoolean(GameRules.RULE_DO_PATROL_SPAWNING)
            );
            return 0;
        }

        RandomSource random = level.getRandom();
        this.nextTick--;
        if (this.nextTick > 0) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: cooldown remaining={}", this.nextTick);
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

        long day = level.getDayTime() / 24000L;
        if (day < Config.factionRaidMinimumDays()) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: day={} < minimumDay={}", day, Config.factionRaidMinimumDays());
            return 0;
        }
        if (level.players().isEmpty()) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: no players in level");
            return 0;
        }

        Player randomPlayer = level.players().get(random.nextInt(level.players().size()));
        if (randomPlayer.isSpectator() || level.isCloseToVillage(randomPlayer.blockPosition(), 2)) {
            JustEnoughGuns.LOGGER.debug(
                    "[FactionRaid] Skip natural raid tick: player={} spectator={} nearVillage={}",
                    randomPlayer.getGameProfile().getName(),
                    randomPlayer.isSpectator(),
                    level.isCloseToVillage(randomPlayer.blockPosition(), 2)
            );
            return 0;
        }

        Faction faction = FactionSpawnHelper.getRandomFaction();
        if (faction == null) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip natural raid tick: no faction selected");
            return 0;
        }

        JustEnoughGuns.LOGGER.debug(
                "[FactionRaid] Summoning natural raid: faction={} pos={} intervalDays={} nextTick={}",
                faction.getName(), randomPlayer.blockPosition(), Config.factionRaidIntervalDays(), this.nextTick
        );
        FactionRaidManager.startRaid(level, faction, randomPlayer.position(), true);
        return 1;
    }
}


