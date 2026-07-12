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
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.config.ServerConfigEditor;
import ttv.migami.jeg.faction.Faction;
import ttv.migami.jeg.faction.FactionSpawnHelper;
import ttv.migami.jeg.faction.GunnerManager;
import ttv.migami.jeg.faction.patrol.PatrolEncounterManager;
import ttv.migami.jeg.gun.GunDefinitions;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;

public final class ModCommands {
    private static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        List<String> factionConfigs = GunnerManager.getConfigFactions();
        for (String factionConfig : factionConfigs) {
            String factionName = factionConfig.split("\\|")[0];
            builder.suggest(factionName);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> GUNNER_TYPE_SUGGESTIONS = (context, builder) -> {
        for (String type : Config.gunnerGrowthTypes()) {
            builder.suggest(type);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> GUNNER_GROWTH_SETTING_SUGGESTIONS = (context, builder) -> {
        for (String setting : Config.gunnerGrowthSettings()) {
            builder.suggest(setting);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> BLOCK_ID_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        for (var block : BuiltInRegistries.BLOCK) {
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (blockId.startsWith(remaining)) {
                builder.suggest(blockId);
            }
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> VEHICLE_ID_SUGGESTIONS = (context, builder) -> {
        VehicleDataManager.all().keySet().stream()
                .sorted()
                .map(ResourceLocation::toString)
                .forEach(builder::suggest);
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
                        .then(giveVehicleContainerCommand())
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

    private static LiteralArgumentBuilder<CommandSourceStack> giveVehicleContainerCommand() {
        return Commands.literal("giveVehicleContainer")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("vehicleId", StringArgumentType.string())
                                .suggests(VEHICLE_ID_SUGGESTIONS)
                                .executes(context -> executeGiveVehicleContainer(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "vehicleId")))));
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
                .then(configCombatCommand())
                .then(configVehicleCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configUiCommand() {
        return Commands.literal("ui")
                .then(configBooleanConfigCommand("crosshair", "ui.showCrosshair"))
                .then(configBooleanConfigCommand("hitFeedback", "ui.showHitFeedback"))
                .then(configBooleanConfigCommand("hideMedals", "ui.hideMedals"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configMobCommand() {
        return Commands.literal("mob")
                .then(configMobSpawnCommand())
                .then(configMobMechanismCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configMobMechanismCommand() {
        return Commands.literal("mechanism")
                .then(configScaledMobChanceCommand("terror", "mob.mechanism.terrorPhantom.chance", "mob.mechanism.terrorPhantom.maxChance"))
                .then(configPhantomGunnerCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configMobSpawnCommand() {
        return Commands.literal("spawn")
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(GUNNER_TYPE_SUGGESTIONS)
                        .then(Commands.argument("setting", StringArgumentType.word())
                                .suggests(GUNNER_GROWTH_SETTING_SUGGESTIONS)
                                .executes(context -> executeGetGunnerGrowthConfig(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "type"),
                                        StringArgumentType.getString(context, "setting")))
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(-1.0D, 5000.0D))
                                        .executes(context -> executeSetGunnerGrowthConfig(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "type"),
                                                StringArgumentType.getString(context, "setting"),
                                                DoubleArgumentType.getDouble(context, "value"))))));
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
                .then(configBooleanConfigCommand("headshotMultiplier", "combat.headshotMultiplier"))
                .then(configGunnerTerrainPlacementCommand())
                .then(configGunnerTerrainBreakCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configVehicleCommand() {
        return Commands.literal("vehicle")
                .then(configBooleanConfigCommand("enabled", "vehicle.enabled"))
                .then(Commands.literal("enemySpawning")
                        .then(configBooleanConfigCommand("enabled", "vehicle.enemySpawning.enabled"))
                        .then(configIntConfigCommand("startDay", "vehicle.enemySpawning.startDay", 0, 5000))
                        .then(configDoubleConfigCommand("conversionChance", "vehicle.enemySpawning.conversionChance"))
                        .then(configDoubleConfigCommand("maxConversionChance", "vehicle.enemySpawning.maxConversionChance"))
                        .then(configDoubleConfigCommand("conversionChancePerDay", "vehicle.enemySpawning.conversionChancePerDay")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configScaledMobChanceCommand(String name, String chanceKey, String maxChanceKey) {
        return Commands.literal(name)
                .then(configDoubleConfigCommand("chance", chanceKey))
                .then(configDoubleConfigCommand("max", maxChanceKey));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configPhantomGunnerCommand() {
        return Commands.literal("phantom")
                .then(configBooleanConfigCommand("deathExplosion", "mob.mechanism.phantomGunner.deathExplosion"));
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

    private static LiteralArgumentBuilder<CommandSourceStack> configIntConfigCommand(String name, String key, int min, int max) {
        return Commands.literal(name)
                .executes(context -> executeGetIntConfig(context.getSource(), key, key))
                .then(Commands.argument("value", IntegerArgumentType.integer(min, max))
                        .executes(context -> executeSetIntConfig(
                                context.getSource(),
                                key,
                                IntegerArgumentType.getInteger(context, "value"),
                                key)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configBlockIdConfigCommand(String name, String key) {
        return Commands.literal(name)
                .executes(context -> executeGetStringConfig(context.getSource(), key, key))
                .then(Commands.argument("value", StringArgumentType.greedyString())
                        .suggests(BLOCK_ID_SUGGESTIONS)
                        .executes(context -> executeSetBlockIdConfig(
                                context.getSource(),
                                key,
                                StringArgumentType.getString(context, "value"),
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

    private static LiteralArgumentBuilder<CommandSourceStack> configGunnerTerrainPlacementCommand() {
        return Commands.literal("gunnerTerrainPlacement")
                .then(configBooleanConfigCommand("enabled", "combat.gunnerTerrainPlacement.enabled"))
                .then(configBlockIdConfigCommand("block", "combat.gunnerTerrainPlacement.block"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configGunnerTerrainBreakCommand() {
        return Commands.literal("gunnerTerrainBreak")
                .then(configIntConfigCommand("maxTier", "combat.gunnerTerrainBreak.maxTier", 0, 3));
    }

    private static int executeUnlockGunRecipes(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("commands.jeg.player_only"));
            return 0;
        }

        List<ResourceKey<Recipe<?>>> gunRecipeKeys = ModItems.unlockGunRecipeKeys();

        if (gunRecipeKeys.isEmpty()) {
            source.sendFailure(Component.translatable("commands.jeg.no_recipes"));
            return 0;
        }

        player.awardRecipesByKey(gunRecipeKeys.stream().map(ResourceKey::location).toList());
        source.sendSuccess(() -> Component.translatable("commands.jeg.recipes_unlocked", gunRecipeKeys.size()), false);
        return 1;
    }

    private static int executeGiveVehicleContainer(CommandSourceStack source, ServerPlayer player, String vehicleIdValue) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }

        ResourceLocation vehicleId = ResourceLocation.tryParse(vehicleIdValue);
        if (vehicleId == null || !VehicleDataManager.all().containsKey(vehicleId)) {
            source.sendFailure(Component.translatable("commands.jeg.unknown_vehicle", vehicleIdValue));
            return 0;
        }

        ItemStack stack = VehicleContainerBlockEntity.createItemForVehicle(vehicleId);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.translatable("commands.jeg.gave_vehicle_container", vehicleId, player.getScoreboardName()), true);
        return 1;
    }

    private static int executeGetBooleanConfig(CommandSourceStack source, String key, String displayName) {
        boolean enabled = (Boolean) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_value", displayName, enabled), false);
        return 1;
    }

    private static int executeGetIntConfig(CommandSourceStack source, String key, String displayName) {
        int value = (Integer) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_value", displayName, value), false);
        return 1;
    }

    private static int executeGetDoubleConfig(CommandSourceStack source, String key, String displayName) {
        double value = (Double) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_value", displayName, value), false);
        return 1;
    }

    private static int executeGetStringConfig(CommandSourceStack source, String key, String displayName) {
        String value = (String) Config.getConfigValue(key);
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_value", displayName, value), false);
        return 1;
    }

    private static int executeGetGunnerGrowthConfig(CommandSourceStack source, String type, String setting) {
        try {
            String key = Config.gunnerGrowthCommandKey(type, setting);
            double value = (Double) Config.getConfigValue(key);
            source.sendSuccess(() -> Component.translatable("commands.jeg.config_value", key, value), false);
            return 1;
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int executeSetBooleanConfig(CommandSourceStack source, String key, boolean value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }
        boolean oldValue = (Boolean) Config.getConfigValue(key);
        ServerConfigEditor.apply(source.getServer(), Map.of(key, Boolean.toString(value)));
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_set", displayName, oldValue, value), true);
        return 1;
    }

    private static int executeSetIntConfig(CommandSourceStack source, String key, int value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }
        int oldValue = (Integer) Config.getConfigValue(key);
        ServerConfigEditor.apply(source.getServer(), Map.of(key, Integer.toString(value)));
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_set", displayName, oldValue, value), true);
        return 1;
    }

    private static int executeSetDoubleConfig(CommandSourceStack source, String key, double value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }
        double oldValue = (Double) Config.getConfigValue(key);
        ServerConfigEditor.apply(source.getServer(), Map.of(key, Double.toString(value)));
        source.sendSuccess(() -> Component.translatable("commands.jeg.config_set", displayName, oldValue, value), true);
        return 1;
    }

    private static int executeSetBlockIdConfig(CommandSourceStack source, String key, String value, String displayName) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }
        try {
            String oldValue = (String) Config.getConfigValue(key);
            ServerConfigEditor.apply(source.getServer(), Map.of(key, value));
            String normalizedValue = (String) Config.getConfigValue(key);
            source.sendSuccess(() -> Component.translatable("commands.jeg.config_set", displayName, oldValue, normalizedValue), true);
            return 1;
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int executeSetGunnerGrowthConfig(CommandSourceStack source, String type, String setting, double value) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }
        try {
            String key = Config.gunnerGrowthCommandKey(type, setting);
            double oldValue = (Double) Config.getConfigValue(key);
            ServerConfigEditor.apply(source.getServer(), Map.of(key, Double.toString(value)));
            source.sendSuccess(() -> Component.translatable("commands.jeg.config_set", key, oldValue, value), true);
            return 1;
        } catch (IllegalArgumentException ex) {
            source.sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }
    }

    private static int executeSpawnPatrol(CommandSourceStack source, String factionName, int size, Vec3 pos, boolean forceGuns, int spread) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.translatable("commands.jeg.peaceful"));
            return 0;
        }

        Faction faction = GunnerManager.getInstance().getFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.translatable("commands.jeg.faction_missing", factionName));
            return 0;
        }

        BlockPos origin = BlockPos.containing(pos);
        BlockPos.MutableBlockPos spawnPos = origin.mutable();
        List<Mob> spawnedMobs = FactionSpawnHelper.spawnPatrol(level, faction, size, null, spawnPos, spread, forceGuns);
        int spawned = spawnedMobs.size();

        if (spawned <= 0) {
            source.sendFailure(Component.translatable("commands.jeg.no_patrol_spawned", FactionSpawnHelper.getLastPatrolDebug()));
            return 0;
        }

        PatrolEncounterManager.startEncounter(level, faction, origin, spawnedMobs);
        final int spawnedCount = spawned;
        source.sendSuccess(() -> Component.translatable("commands.jeg.spawned_patrol", factionName, spawnedCount), true);
        return 1;
    }

    private static int executeSimulatePatrol(CommandSourceStack source, String factionName, int size, Player player, boolean forceGuns) {
        if (!source.hasPermission(2)) {
            source.sendFailure(Component.translatable("commands.jeg.no_permission"));
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.translatable("commands.jeg.peaceful"));
            return 0;
        }

        Faction faction = GunnerManager.getInstance().getFactionByName(factionName);
        if (faction == null) {
            source.sendFailure(Component.translatable("commands.jeg.faction_missing", factionName));
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
            source.sendFailure(Component.translatable("commands.jeg.no_patrol_spawned", FactionSpawnHelper.getLastPatrolDebug()));
            return 0;
        }

        PatrolEncounterManager.startEncounter(level, faction, origin, spawnedMobs);
        final int spawnedCount = spawned;
        source.sendSuccess(() -> Component.translatable("commands.jeg.simulated_patrol", factionName, spawnedCount), true);
        return 1;
    }
}
