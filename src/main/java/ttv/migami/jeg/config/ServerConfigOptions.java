package ttv.migami.jeg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ttv.migami.jeg.Config;

public final class ServerConfigOptions {
    public enum Category {
        UI("gui.jegn.config.category.ui"),
        PATROL("gui.jegn.config.category.patrol"),
        MOB("gui.jegn.config.category.mob"),
        COMBAT("gui.jegn.config.category.combat"),
        VEHICLE("gui.jegn.config.category.vehicle");

        private final String translationKey;

        Category(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    public enum ValueType {
        BOOLEAN,
        INTEGER,
        DOUBLE,
        BLOCK_ID
    }

    public record Option(
            String key,
            Category category,
            ValueType type,
            double min,
            double max,
            String labelKey,
            String growthType
    ) {
        public boolean hasRange() {
            return !Double.isNaN(this.min) && !Double.isNaN(this.max);
        }

        public boolean isGrowthOption() {
            return !this.growthType.isEmpty();
        }
    }

    private static final List<Option> OPTIONS = buildOptions();
    private static final Map<String, Option> OPTIONS_BY_KEY = indexOptions(OPTIONS);

    private ServerConfigOptions() {}

    public static List<Option> all() {
        return OPTIONS;
    }

    public static Option require(String key) {
        Option option = OPTIONS_BY_KEY.get(key);
        if (option == null) {
            throw new IllegalArgumentException("Unknown editable config key: " + key);
        }
        return option;
    }

    public static List<Option> forCategory(Category category, String growthType) {
        return OPTIONS.stream()
                .filter(option -> option.category() == category)
                .filter(option -> !option.isGrowthOption() || option.growthType().equals(growthType))
                .toList();
    }

    private static List<Option> buildOptions() {
        List<Option> options = new ArrayList<>();

        addBoolean(options, "ui.showCrosshair", Category.UI, "gui.jegn.config.option.show_crosshair");
        addBoolean(options, "ui.showHitFeedback", Category.UI, "gui.jegn.config.option.show_hit_feedback");

        addBoolean(options, "patrol.enabled", Category.PATROL, "gui.jegn.config.option.patrol_enabled");
        addInteger(options, "patrol.intervalDays", Category.PATROL, 0, 30, "gui.jegn.config.option.patrol_interval_days");
        addInteger(options, "patrol.minimumDays", Category.PATROL, 0, 100, "gui.jegn.config.option.patrol_minimum_days");
        addDouble(options, "patrol.spawnChance", Category.PATROL, 0.0D, 1.0D, "gui.jegn.config.option.patrol_spawn_chance");

        addDouble(options, "mob.mechanism.terrorPhantom.chance", Category.MOB, 0.0D, 1.0D, "gui.jegn.config.option.terror_phantom_chance");
        addDouble(options, "mob.mechanism.terrorPhantom.maxChance", Category.MOB, 0.0D, 1.0D, "gui.jegn.config.option.terror_phantom_max_chance");
        addBoolean(options, "mob.mechanism.phantomGunner.deathExplosion", Category.MOB, "gui.jegn.config.option.phantom_gunner_death_explosion");
        for (String type : Config.gunnerGrowthTypes()) {
            for (String setting : Config.gunnerGrowthSettings()) {
                options.add(new Option(
                        Config.gunnerGrowthCommandKey(type, setting),
                        Category.MOB,
                        ValueType.DOUBLE,
                        -1.0D,
                        5000.0D,
                        "gui.jegn.config.growth.setting." + setting,
                        type
                ));
            }
        }

        addBoolean(options, "combat.bulletBlockDestruction", Category.COMBAT, "gui.jegn.config.option.bullet_block_destruction");
        addBoolean(options, "combat.magazineFeed", Category.COMBAT, "gui.jegn.config.option.magazine_feed");
        addBoolean(options, "combat.headshotMultiplier", Category.COMBAT, "gui.jegn.config.option.headshot_multiplier");
        addBoolean(options, "combat.gunnerTerrainPlacement.enabled", Category.COMBAT, "gui.jegn.config.option.gunner_terrain_placement");
        options.add(new Option(
                "combat.gunnerTerrainPlacement.block",
                Category.COMBAT,
                ValueType.BLOCK_ID,
                Double.NaN,
                Double.NaN,
                "gui.jegn.config.option.gunner_terrain_block",
                ""
        ));
        addInteger(options, "combat.gunnerTerrainBreak.maxTier", Category.COMBAT, 0, 3, "gui.jegn.config.option.gunner_terrain_break_tier");

        addBoolean(options, "vehicle.enabled", Category.VEHICLE, "gui.jegn.config.option.vehicle_enabled");
        addBoolean(options, "vehicle.enemySpawning.enabled", Category.VEHICLE, "gui.jegn.config.option.enemy_vehicle_spawning");
        addInteger(options, "vehicle.enemySpawning.startDay", Category.VEHICLE, 0, 5000, "gui.jegn.config.option.enemy_vehicle_start_day");
        addDouble(options, "vehicle.enemySpawning.conversionChance", Category.VEHICLE, 0.0D, 1.0D, "gui.jegn.config.option.enemy_vehicle_conversion_chance");

        if (options.size() != 199) {
            throw new IllegalStateException("Expected 199 editable config options, found " + options.size());
        }
        return List.copyOf(options);
    }

    private static Map<String, Option> indexOptions(List<Option> options) {
        Map<String, Option> indexed = new LinkedHashMap<>();
        for (Option option : options) {
            if (indexed.put(option.key(), option) != null) {
                throw new IllegalStateException("Duplicate editable config key: " + option.key());
            }
        }
        return Map.copyOf(indexed);
    }

    private static void addBoolean(List<Option> options, String key, Category category, String labelKey) {
        options.add(new Option(key, category, ValueType.BOOLEAN, Double.NaN, Double.NaN, labelKey, ""));
    }

    private static void addInteger(List<Option> options, String key, Category category, int min, int max, String labelKey) {
        options.add(new Option(key, category, ValueType.INTEGER, min, max, labelKey, ""));
    }

    private static void addDouble(List<Option> options, String key, Category category, double min, double max, String labelKey) {
        options.add(new Option(key, category, ValueType.DOUBLE, min, max, labelKey, ""));
    }
}
