package ttv.migami.jeg.faction.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
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

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(ModEffects.FACTION_OMEN)) {
                clearOmenFactionTag(player);
                continue;
            }
            if (player.isSpectator()) {
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

            if (FactionRaidManager.hasActiveRaidNear(currentLevel, respawnPos, 96.0D)) {
                continue;
            }

            Faction faction = resolveFactionFromOmen(player);
            if (faction == null) {
                clearOmenFactionTag(player);
                player.removeEffect(ModEffects.FACTION_OMEN);
                continue;
            }

            FactionRaidManager.startRaid(currentLevel, faction, respawnCenter, true);
            clearOmenFactionTag(player);
            player.removeEffect(ModEffects.FACTION_OMEN);
            player.sendSystemMessage(Component.translatable("message.jeg.faction_raid.home_triggered"));
        }
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
