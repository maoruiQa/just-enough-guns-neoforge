package ttv.migami.jeg.faction.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.init.ModEffects;

public final class HomeRaidTriggerManager {
    private HomeRaidTriggerManager() {}

    public static void tick(MinecraftServer server) {
        if (!Config.factionRaidEnabled()) {
            return;
        }

        int radius = Config.factionRaidHomeTriggerRadius();
        double radiusSq = (double) radius * (double) radius;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(ModEffects.FACTION_OMEN) || player.isSpectator()) {
                continue;
            }

            if (!(player.level() instanceof ServerLevel currentLevel) || currentLevel.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                continue;
            }

            BlockPos respawnPos = player.getRespawnPosition();
            ResourceKey<Level> respawnDimension = player.getRespawnDimension();

            if (respawnPos == null) {
                ServerLevel overworld = server.overworld();
                if (overworld == null) {
                    continue;
                }
                respawnPos = overworld.getSharedSpawnPos();
                respawnDimension = Level.OVERWORLD;
            }

            if (player.level().dimension() != respawnDimension) {
                continue;
            }

            Vec3 respawnCenter = Vec3.atCenterOf(respawnPos);
            if (player.position().distanceToSqr(respawnCenter) > radiusSq) {
                continue;
            }

            if (RaidEntity.hasActiveRaidNear(currentLevel, respawnPos, 96.0D)) {
                continue;
            }

            Faction faction = FactionSpawnHelper.getRandomFaction();
            if (faction == null) {
                continue;
            }

            RaidEntity.summonRaidEntity(currentLevel, faction, respawnCenter, true);
            player.removeEffect(ModEffects.FACTION_OMEN);
            player.sendSystemMessage(Component.translatable("message.jeg.faction_raid.home_triggered"));
        }
    }
}
