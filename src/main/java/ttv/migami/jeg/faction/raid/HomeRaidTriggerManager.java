package ttv.migami.jeg.faction.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.GunnerManager;
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
        var factionOmen = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.FACTION_OMEN.get());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(factionOmen)) {
                clearOmenFactionTag(player);
                continue;
            }
            if (!isEligibleHomeRaidPlayer(player)) {
                continue;
            }

            if (!(player.level() instanceof ServerLevel currentLevel) || currentLevel.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                continue;
            }

            ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
            BlockPos respawnPos = respawnConfig != null ? respawnConfig.respawnData().pos() : null;
            ResourceKey<Level> respawnDimension = respawnConfig != null
                    ? respawnConfig.respawnData().dimension()
                    : Level.OVERWORLD;

            if (respawnPos == null) {
                ServerLevel overworld = server.overworld();
                if (overworld == null) {
                    continue;
                }
                LevelData.RespawnData sharedRespawn = overworld.getRespawnData();
                respawnPos = sharedRespawn.pos();
                respawnDimension = sharedRespawn.dimension();
            }

            if (player.level().dimension() != respawnDimension) {
                continue;
            }

            Vec3 respawnCenter = Vec3.atCenterOf(respawnPos);
            if (player.position().distanceToSqr(respawnCenter) > radiusSq) {
                continue;
            }

            if (FactionRaidManager.hasActiveRaidNear(currentLevel, respawnPos, 96.0D)) {
                continue;
            }

            Faction faction = resolveFactionFromOmen(player);
            if (faction == null) {
                clearOmenFactionTag(player);
                player.removeEffect(factionOmen);
                continue;
            }

            FactionRaidManager.startRaid(currentLevel, faction, respawnCenter, true);
            clearOmenFactionTag(player);
            player.removeEffect(factionOmen);
            player.sendSystemMessage(Component.translatable("message.jeg.faction_raid.home_triggered"));
        }
    }

    private static boolean isEligibleHomeRaidPlayer(ServerPlayer player) {
        return player.isAlive()
                && !player.isDeadOrDying()
                && !player.isSpectator()
                && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
    }

    @Nullable
    private static Faction resolveFactionFromOmen(ServerPlayer player) {
        GunnerManager manager = GunnerManager.getInstance();
        for (String tag : player.entityTags()) {
            if (!tag.startsWith(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX)) {
                continue;
            }
            String factionName = tag.substring(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX.length());
            if (factionName.isBlank()) {
                continue;
            }
            Faction faction = manager.getFactionByName(factionName);
            if (faction != null) {
                return faction;
            }
        }
        return null;
    }

    private static void clearOmenFactionTag(ServerPlayer player) {
        for (String tag : java.util.List.copyOf(player.entityTags())) {
            if (tag.startsWith(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX)) {
                player.removeTag(tag);
            }
        }
    }
}
