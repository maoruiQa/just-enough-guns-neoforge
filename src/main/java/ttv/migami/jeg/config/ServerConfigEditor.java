package ttv.migami.jeg.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.event.FactionEventTicker;
import ttv.migami.jeg.network.NetworkHandler;

public final class ServerConfigEditor {
    public static final class ValidationException extends IllegalArgumentException {
        private final String key;

        public ValidationException(String key, String message) {
            super(message);
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public record ApplyResult(Map<String, String> values, int changedCount) {
        public ApplyResult {
            values = Map.copyOf(values);
        }
    }

    private ServerConfigEditor() {}

    public static Map<String, String> snapshot() {
        Map<String, String> values = new LinkedHashMap<>();
        for (ServerConfigOptions.Option option : ServerConfigOptions.all()) {
            values.put(option.key(), String.valueOf(Config.getConfigValue(option.key())));
        }
        return Map.copyOf(values);
    }

    public static ApplyResult apply(MinecraftServer server, Map<String, String> rawChanges) {
        Objects.requireNonNull(server);
        if (rawChanges.isEmpty()) {
            return new ApplyResult(snapshot(), 0);
        }

        Map<ServerConfigOptions.Option, Object> parsedChanges = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : rawChanges.entrySet()) {
            ServerConfigOptions.Option option;
            try {
                option = ServerConfigOptions.require(entry.getKey());
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(entry.getKey(), ex.getMessage());
            }
            parsedChanges.put(option, parse(option, entry.getValue()));
        }

        int changedCount = 0;
        boolean uiChanged = false;
        boolean patrolIntervalChanged = false;
        for (Map.Entry<ServerConfigOptions.Option, Object> entry : parsedChanges.entrySet()) {
            ServerConfigOptions.Option option = entry.getKey();
            Object currentValue = Config.getConfigValue(option.key());
            Object newValue = entry.getValue();
            if (Objects.equals(currentValue, newValue)) {
                continue;
            }
            Config.setConfigValue(option.key(), newValue);
            changedCount++;
            uiChanged |= option.key().startsWith("ui.");
            patrolIntervalChanged |= option.key().equals("patrol.intervalDays");
        }

        if (changedCount > 0) {
            Config.saveServerConfig();
            if (uiChanged) {
                NetworkHandler.broadcastUiConfig(server);
            }
            if (patrolIntervalChanged) {
                FactionEventTicker.reschedulePatrolSpawner();
            }
        }
        return new ApplyResult(snapshot(), changedCount);
    }

    private static Object parse(ServerConfigOptions.Option option, String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Missing value for " + option.key());
        }
        return switch (option.type()) {
            case BOOLEAN -> parseBoolean(option, rawValue);
            case INTEGER -> parseInteger(option, rawValue);
            case DOUBLE -> parseDouble(option, rawValue);
            case BLOCK_ID -> parseBlockId(option, rawValue);
        };
    }

    private static String parseBlockId(ServerConfigOptions.Option option, String rawValue) {
        try {
            return Config.normalizeGunnerTerrainSupportBlockId(rawValue);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(option.key(), ex.getMessage());
        }
    }

    private static boolean parseBoolean(ServerConfigOptions.Option option, String rawValue) {
        if ("true".equalsIgnoreCase(rawValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(rawValue)) {
            return false;
        }
        throw invalid(option, rawValue);
    }

    private static int parseInteger(ServerConfigOptions.Option option, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < option.min() || value > option.max()) {
                throw invalid(option, rawValue);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw invalid(option, rawValue);
        }
    }

    private static double parseDouble(ServerConfigOptions.Option option, String rawValue) {
        try {
            double value = Double.parseDouble(rawValue);
            if (!Double.isFinite(value) || value < option.min() || value > option.max()) {
                throw invalid(option, rawValue);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw invalid(option, rawValue);
        }
    }

    private static IllegalArgumentException invalid(ServerConfigOptions.Option option, String rawValue) {
        return new ValidationException(option.key(), "Invalid value for " + option.key() + ": " + rawValue);
    }
}
