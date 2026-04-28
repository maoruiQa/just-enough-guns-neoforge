package ttv.migami.jeg.init;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.event.FactionEventTicker;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.network.NetworkHandler;

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

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("justEnoughGuns")
                        .then(unlockGunRecipesCommand())
                        .then(spawnPatrolCommand())
                        .then(simulatePatrolCommand())
                        .then(configCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> unlockGunRecipesCommand() {
        return Commands.literal("unlockGunRecipes")
                .executes(context -> executeUnlockGunRecipes(context.getSource()));
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

    private static LiteralArgumentBuilder<CommandSourceStack> configCommand() {
        return Commands.literal("config")
                .then(configUiCommand())
                .then(configPatrolCommand())
                .then(configMobCommand())
                .then(configCombatCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configUiCommand() {
        return Commands.literal("ui")
                .then(configBooleanConfigCommand("crosshair", "ui.showCrosshair"))
                .then(configBooleanConfigCommand("hitFeedback", "ui.showHitFeedback"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configMobCommand() {
        return Commands.literal("mob")
                .then(configScaledMobChanceCommand("terror", "mob.terrorPhantom.chance", "mob.terrorPhantom.maxChance"))
                .then(configScaledMobChanceCommand("phantom", "mob.phantomGunner.chance", "mob.phantomGunner.maxChance"))
                .then(configScaledMobChanceCommand("pillager", "mob.pillagerGunner.chance", "mob.pillagerGunner.maxChance"))
                .then(configSimpleMobChanceCommand("skeleton", "mob.skeletonGunner.chance"))
                .then(configSimpleMobChanceCommand("zombie", "mob.zombieGunner.chance"))
                .then(configSimpleMobChanceCommand("husk", "mob.huskGunner.chance"))
                .then(configSimpleMobChanceCommand("zombifiedPiglin", "mob.zombifiedPiglinGunner.chance"))
                .then(configSimpleMobChanceCommand("piglin", "mob.piglinGunner.chance"))
                .then(configSimpleMobChanceCommand("witherSkeleton", "mob.witherSkeletonGunner.chance"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPatrolCommand() {
        return Commands.literal("patrol")
                .then(configPatrolEnabledCommand())
                .then(configPatrolIntervalDaysCommand())
                .then(configPatrolMinimumDaysCommand())
                .then(configPatrolSpawnChanceCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configCombatCommand() {
        return Commands.literal("combat")
                .then(configBulletBlockDestructionCommand())
                .then(configMagazineFeedCommand())
                .then(configGunnerTerrainCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configScaledMobChanceCommand(String name, String chanceKey, String maxChanceKey) {
        return Commands.literal(name)
                .then(configDoubleConfigCommand("chance", chanceKey))
                .then(configDoubleConfigCommand("max", maxChanceKey));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configSimpleMobChanceCommand(String name, String chanceKey) {
        return Commands.literal(name)
                .then(configDoubleConfigCommand("chance", chanceKey));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configDoubleConfigCommand(String name, String key) {
        return Commands.literal(name)
                .executes(context -> executeGetDoubleConfig(context.getSource(), key, key))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                        .executes(context -> executeSetDoubleConfig(
                                context.getSource(),
                                key,
                                DoubleArgumentType.getDouble(context, "value"),
                                key)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configBooleanConfigCommand(String name, String key) {
        return Commands.literal(name)
                .executes(context -> executeGetBooleanConfig(context.getSource(), key, key))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> executeSetBooleanConfig(
                                context.getSource(),
                                key,
                                BoolArgumentType.getBool(context, "value"),
                                key)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPatrolEnabledCommand() {
        return Commands.literal("enabled")
                .executes(context -> executeGetBooleanConfig(context.getSource(), "patrol.enabled", "patrol.enabled"))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> executeSetBooleanConfig(
                                context.getSource(),
                                "patrol.enabled",
                                BoolArgumentType.getBool(context, "value"),
                                "patrol.enabled")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPatrolIntervalDaysCommand() {
        return Commands.literal("intervalDays")
                .executes(context -> executeGetIntConfig(context.getSource(), "patrol.intervalDays", "patrol.intervalDays"))
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 30))
                        .executes(context -> executeSetIntConfig(
                                context.getSource(),
                                "patrol.intervalDays",
                                IntegerArgumentType.getInteger(context, "value"),
                                "patrol.intervalDays")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPatrolMinimumDaysCommand() {
        return Commands.literal("minimumDays")
                .executes(context -> executeGetIntConfig(context.getSource(), "patrol.minimumDays", "patrol.minimumDays"))
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(context -> executeSetIntConfig(
                                context.getSource(),
                                "patrol.minimumDays",
                                IntegerArgumentType.getInteger(context, "value"),
                                "patrol.minimumDays")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPatrolSpawnChanceCommand() {
        return Commands.literal("spawnChance")
                .executes(context -> executeGetDoubleConfig(context.getSource(), "patrol.spawnChance", "patrol.spawnChance"))
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                        .executes(context -> executeSetDoubleConfig(
                                context.getSource(),
                                "patrol.spawnChance",
                                DoubleArgumentType.getDouble(context, "value"),
                                "patrol.spawnChance")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configBulletBlockDestructionCommand() {
        return Commands.literal("blockDamage")
                .executes(context -> executeGetBooleanConfig(context.getSource(), "combat.bulletBlockDestruction", "combat.bulletBlockDestruction"))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> executeSetBooleanConfig(
                                context.getSource(),
                                "combat.bulletBlockDestruction",
                                BoolArgumentType.getBool(context, "value"),
                                "combat.bulletBlockDestruction")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configMagazineFeedCommand() {
        return Commands.literal("magazineFeed")
                .executes(context -> executeGetBooleanConfig(context.getSource(), "combat.magazineFeed", "combat.magazineFeed"))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> executeSetBooleanConfig(
                                context.getSource(),
                                "combat.magazineFeed",
                                BoolArgumentType.getBool(context, "value"),
                                "combat.magazineFeed")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configGunnerTerrainCommand() {
        return Commands.literal("gunnerTerrain")
                .then(Commands.literal("enabled")
                        .executes(context -> executeGetBooleanConfig(context.getSource(), "combat.gunnerTerrain.enabled", "combat.gunnerTerrain.enabled"))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> executeSetBooleanConfig(
                                        context.getSource(),
                                        "combat.gunnerTerrain.enabled",
                                        BoolArgumentType.getBool(context, "value"),
                                        "combat.gunnerTerrain.enabled"))))
                .then(Commands.literal("maxTier")
                        .executes(context -> executeGetIntConfig(context.getSource(), "combat.gunnerTerrain.maxTier", "combat.gunnerTerrain.maxTier"))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 3))
                                .executes(context -> executeSetIntConfig(
                                        context.getSource(),
                                        "combat.gunnerTerrain.maxTier",
                                        IntegerArgumentType.getInteger(context, "value"),
                                        "combat.gunnerTerrain.maxTier"))));
    }

    private static int executeUnlockGunRecipes(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player"));
            return 0;
        }

        List<ResourceKey<Recipe<?>>> gunRecipeKeys = ModItems.unlockGunRecipeKeys();

        if (gunRecipeKeys.isEmpty()) {
            source.sendFailure(Component.literal("No gun or coolant recipes were found"));
            return 0;
        }

        player.awardRecipesByKey(gunRecipeKeys.stream().map(ResourceKey::location).toList());
        source.sendSuccess(() -> Component.literal("Unlocked " + gunRecipeKeys.size() + " gun/coolant recipes."), false);
        return 1;
    }

    private static int executeGetBooleanConfig(CommandSourceStack source, String key, String displayName) {
        boolean enabled = (Boolean) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.literal(displayName + " = " + enabled), false);
        return 1;
    }

    private static int executeGetIntConfig(CommandSourceStack source, String key, String displayName) {
        int value = (Integer) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.literal(displayName + " = " + value), false);
        return 1;
    }

    private static int executeGetDoubleConfig(CommandSourceStack source, String key, String displayName) {
        double value = (Double) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.literal(displayName + " = " + value), false);
        return 1;
    }

    private static int executeSetBooleanConfig(CommandSourceStack source, String key, boolean value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }
        boolean oldValue = (Boolean) Config.getConfigValue(key);
        Config.setConfigValue(key, value);
        Config.saveServerConfig();
        if (key.startsWith("ui.")) {
            NetworkHandler.broadcastUiConfig(source.getServer());
        }
        source.sendSuccess(() -> Component.literal("Set " + displayName + " from " + oldValue + " to " + value), true);
        return 1;
    }

    private static int executeSetIntConfig(CommandSourceStack source, String key, int value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }
        int oldValue = (Integer) Config.getConfigValue(key);
        Config.setConfigValue(key, value);
        Config.saveServerConfig();
        if ("patrol.intervalDays".equals(key)) {
            FactionEventTicker.reschedulePatrolSpawner();
        }
        source.sendSuccess(() -> Component.literal("Set " + displayName + " from " + oldValue + " to " + value), true);
        return 1;
    }

    private static int executeSetDoubleConfig(CommandSourceStack source, String key, double value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.literal("You do not have permission to execute this command"));
            return 0;
        }
        double oldValue = (Double) Config.getConfigValue(key);
        Config.setConfigValue(key, value);
        Config.saveServerConfig();
        source.sendSuccess(() -> Component.literal("Set " + displayName + " from " + oldValue + " to " + value), true);
        return 1;
    }

    private static int executeSpawnPatrol(CommandSourceStack source, String factionName, int size, Vec3 pos, boolean forceGuns, int spread) {
        if (!source.hasPermission(2)) {
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
        if (!source.hasPermission(2)) {
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

        RandomSource random = RandomSource.create(player.getUUID().getLeastSignificantBits() ^ level.getGameTime() ^ player.blockPosition().asLong());
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
