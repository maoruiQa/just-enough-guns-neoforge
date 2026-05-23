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
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.JustEnoughGuns;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.init.ModEffects;

public final class HomeRaidTriggerManager {
    private HomeRaidTriggerManager() {}

    public static void tick(MinecraftServer server) {
        if (!Config.factionRaidEnabled()) {
            JustEnoughGuns.LOGGER.debug("[FactionRaid] Skip home-trigger tick: config disabled");
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
                JustEnoughGuns.LOGGER.debug(
                        "[FactionRaid] Home-trigger blocked: player={} gameMode={} spectator={}",
                        player.getGameProfile().getName(),
                        player.gameMode.getGameModeForPlayer(),
                        player.isSpectator()
                );
                continue;
            }

            if (!(player.level() instanceof ServerLevel currentLevel) || currentLevel.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
                JustEnoughGuns.LOGGER.debug(
                        "[FactionRaid] Home-trigger blocked: player={} invalid level or peaceful",
                        player.getGameProfile().getName()
                );
                continue;
            }

            BlockPos respawnPos = player.getRespawnPosition();
            ResourceKey<Level> respawnDimension = player.getRespawnDimension();

            if (respawnPos == null) {
                ServerLevel overworld = server.overworld();
                if (overworld == null) {
                    JustEnoughGuns.LOGGER.debug("[FactionRaid] Home-trigger blocked: player={} has no respawn and no overworld", player.getGameProfile().getName());
                    continue;
                }
                respawnPos = overworld.getSharedSpawnPos();
                respawnDimension = Level.OVERWORLD;
            }

            if (player.level().dimension() != respawnDimension) {
                JustEnoughGuns.LOGGER.debug(
                        "[FactionRaid] Home-trigger blocked: player={} wrong dimension current={} expected={}",
                        player.getGameProfile().getName(),
                        player.level().dimension().location(),
                        respawnDimension.location()
                );
                continue;
            }

            Vec3 respawnCenter = Vec3.atCenterOf(respawnPos);
            if (player.position().distanceToSqr(respawnCenter) > radiusSq) {
                JustEnoughGuns.LOGGER.debug(
                        "[FactionRaid] Home-trigger blocked: player={} outside radius={} respawn={}",
                        player.getGameProfile().getName(),
                        radius,
                        respawnPos
                );
                continue;
            }

            if (FactionRaidManager.hasActiveRaidNear(currentLevel, respawnPos, 96.0D)) {
                JustEnoughGuns.LOGGER.debug("[FactionRaid] Home-trigger blocked: player={} nearby active raid at {}", player.getGameProfile().getName(), respawnPos);
                continue;
            }

            Faction faction = resolveFactionFromOmen(player);
            if (faction == null) {
                JustEnoughGuns.LOGGER.debug("[FactionRaid] Home-trigger blocked: player={} omen has no faction tag", player.getGameProfile().getName());
                clearOmenFactionTag(player);
                player.removeEffect(factionOmen);
                continue;
            }

            JustEnoughGuns.LOGGER.debug(
                    "[FactionRaid] Home-trigger summon: player={} faction={} respawn={} dim={}",
                    player.getGameProfile().getName(),
                    faction.getName(),
                    respawnPos,
                    currentLevel.dimension().location()
            );
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
        for (String tag : player.getTags()) {
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
        for (String tag : java.util.List.copyOf(player.getTags())) {
            if (tag.startsWith(FactionSpawnHelper.OMEN_FACTION_TAG_PREFIX)) {
                player.removeTag(tag);
            }
        }
    }
}
