package ttv.migami.jeg.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;

public final class ModCommands {
    private static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        List<String> factionConfigs = GunnerManager.getConfigFactions();
        for (String factionConfig : factionConfigs) {
            String factionName = factionConfig.split("\\|")[0];
            builder.suggest(factionName);
        }
        return builder.buildFuture();
    };

    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("justEnoughGuns")
                        .then(spawnPatrolCommand())
                        .then(simulatePatrolCommand())
                        .then(bulletBlockDestructionCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> spawnPatrolCommand() {
        return Commands.literal("spawnPatrol")
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_SUGGESTIONS)
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 20))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(context -> executeSpawnPatrol(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "faction"),
                                                IntegerArgumentType.getInteger(context, "size"),
                                                Vec3Argument.getVec3(context, "pos"),
                                                true,
                                                10))
                                        .then(Commands.argument("forceGuns", BoolArgumentType.bool())
                                                .executes(context -> executeSpawnPatrol(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "faction"),
                                                        IntegerArgumentType.getInteger(context, "size"),
                                                        Vec3Argument.getVec3(context, "pos"),
                                                        BoolArgumentType.getBool(context, "forceGuns"),
                                                        10))
                                                .then(Commands.argument("spawnRadius", IntegerArgumentType.integer(0, 16))
                                                        .executes(context -> executeSpawnPatrol(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "faction"),
                                                                IntegerArgumentType.getInteger(context, "size"),
                                                                Vec3Argument.getVec3(context, "pos"),
                                                                BoolArgumentType.getBool(context, "forceGuns"),
                                                                IntegerArgumentType.getInteger(context, "spawnRadius"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> simulatePatrolCommand() {
        return Commands.literal("simulatePatrol")
                .then(Commands.argument("faction", StringArgumentType.string())
                        .suggests(FACTION_SUGGESTIONS)
                        .then(Commands.argument("size", IntegerArgumentType.integer(1, 20))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> executeSimulatePatrol(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "faction"),
                                                IntegerArgumentType.getInteger(context, "size"),
                                                EntityArgument.getPlayer(context, "player"),
                                                true))
                                        .then(Commands.argument("forceGuns", BoolArgumentType.bool())
                                                .executes(context -> executeSimulatePatrol(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "faction"),
                                                        IntegerArgumentType.getInteger(context, "size"),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        BoolArgumentType.getBool(context, "forceGuns")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> bulletBlockDestructionCommand() {
        return Commands.literal("bulletBlockDestruction")
                .executes(context -> executeGetBulletBlockDestruction(context.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> executeSetBulletBlockDestruction(
                                context.getSource(),
                                BoolArgumentType.getBool(context, "enabled"))));
    }

    private static int executeGetBulletBlockDestruction(CommandSourceStack source) {
        boolean enabled = Config.bulletBlockDestructionEnabled();
        source.sendSuccess(
                () -> Component.literal("Bullet block destruction is " + (enabled ? "enabled" : "disabled")),
                false
        );
        return 1;
    }

    private static int executeSetBulletBlockDestruction(CommandSourceStack source, boolean enabled) {
        if (!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }

        Config.setBulletBlockDestructionEnabled(enabled);
        source.sendSuccess(
                () -> Component.literal("Set bullet block destruction to " + enabled),
                true
        );
        return 1;
    }

    private static int executeSpawnPatrol(CommandSourceStack source, String factionName, int size, Vec3 pos, boolean forceGuns, int spread) {
        if (!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.literal("Mobs can't spawn in Peaceful"));
            return 0;
        }

        Faction faction = GunnerManager.getInstance().getFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.literal("Faction '" + factionName + "' does not exist"));
            return 0;
        }

        BlockPos origin = BlockPos.containing(pos);
        BlockPos.MutableBlockPos spawnPos = origin.mutable();
        List<Mob> spawnedMobs = FactionSpawnHelper.spawnPatrol(level, faction, size, null, spawnPos, spread, forceGuns);
        int spawned = spawnedMobs.size();

        if (spawned <= 0) {
            source.sendFailure(Component.literal("No patrol members were spawned; debug=" + FactionSpawnHelper.getLastPatrolDebug()));
            return 0;
        }

        PatrolEncounterManager.startEncounter(level, faction, origin, spawnedMobs);
        final int spawnedCount = spawned;
        source.sendSuccess(() -> Component.literal("Spawned patrol: faction=" + factionName + ", count=" + spawnedCount), true);
        return 1;
    }

    private static int executeSimulatePatrol(CommandSourceStack source, String factionName, int size, Player player, boolean forceGuns) {
        if (!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.literal("Mobs can't spawn in Peaceful"));
            return 0;
        }

        Faction faction = GunnerManager.getInstance().getFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.literal("Faction '" + factionName + "' does not exist"));
            return 0;
        }

        RandomSource random = level.random;
        BlockPos.MutableBlockPos center = player.blockPosition().mutable().move(
                (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1),
                0,
                (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1));

        Vec3 centerPos = new Vec3(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
        BlockPos origin = BlockPos.containing(centerPos);
        BlockPos.MutableBlockPos spawnPos = origin.mutable();
        List<Mob> spawnedMobs = FactionSpawnHelper.spawnPatrol(level, faction, size, player, spawnPos, 6, forceGuns);
        int spawned = spawnedMobs.size();

        if (spawned <= 0) {
            source.sendFailure(Component.literal("No patrol members were spawned; debug=" + FactionSpawnHelper.getLastPatrolDebug()));
            return 0;
        }

        PatrolEncounterManager.startEncounter(level, faction, origin, spawnedMobs);
        final int spawnedCount = spawned;
        source.sendSuccess(() -> Component.literal("Simulated patrol: faction=" + factionName + ", count=" + spawnedCount), true);
        return 1;
    }
}
