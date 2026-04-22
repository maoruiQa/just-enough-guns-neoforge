package ttv.migami.jeg.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class ModConfigSpec {
    private final List<Value<?>> values;

    private ModConfigSpec(List<Value<?>> values) {
        this.values = List.copyOf(values);
    }

    public void load(Path path) {
        ensureParentExists(path);
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        try {
            if (Files.exists(path)) {
                config.load();
            }
            for (Value<?> value : values) {
                value.loadFrom(config);
            }
            config.save();
        } finally {
            config.close();
        }
    }

    public void save(Path path) {
        ensureParentExists(path);
        CommentedFileConfig config = CommentedFileConfig.builder(path).sync().build();
        try {
            if (Files.exists(path)) {
                config.load();
            }
            for (Value<?> value : values) {
                value.saveTo(config);
            }
            config.save();
        } finally {
            config.close();
        }
    }

    private static void ensureParentExists(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create config directory for " + path, e);
        }
    }

    public static class Builder {
        private final Deque<String> sections = new ArrayDeque<>();
        private final List<Value<?>> values = new ArrayList<>();

        public Builder push(String path) {
            sections.addLast(path);
            return this;
        }

        public Builder pop() {
            if (!sections.isEmpty()) {
                sections.removeLast();
            }
            return this;
        }

        public Builder comment(String... comments) {
            return this;
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            DoubleValue value = new DoubleValue(fullPath(path), defaultValue, min, max);
            values.add(value);
            return value;
        }

        public BooleanValue define(String path, boolean defaultValue) {
            BooleanValue value = new BooleanValue(fullPath(path), defaultValue);
            values.add(value);
            return value;
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            IntValue value = new IntValue(fullPath(path), defaultValue, min, max);
            values.add(value);
            return value;
        }

        public ModConfigSpec build() {
            return new ModConfigSpec(values);
        }

        private String fullPath(String path) {
            if (sections.isEmpty()) {
                return path;
            }
            return String.join(".", sections) + "." + path;
        }
    }

    public abstract static class Value<T> {
        private final String path;
        private final T defaultValue;
        private T value;

        protected Value(String path, T defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = normalize(Objects.requireNonNull(value));
        }

        public String path() {
            return path;
        }

        protected abstract T normalize(Object value);

        private void loadFrom(CommentedFileConfig config) {
            Object raw = config.get(path);
            if (raw == null) {
                value = defaultValue;
                config.set(path, defaultValue);
                return;
            }
            value = normalize(raw);
            config.set(path, value);
        }

        private void saveTo(CommentedFileConfig config) {
            config.set(path, value);
        }
    }

    public static class DoubleValue extends Value<Double> {
        private final double min;
        private final double max;

        public DoubleValue(String path, double value, double min, double max) {
            super(path, value);
            this.min = min;
            this.max = max;
            set(value);
        }

        @Override
        protected Double normalize(Object value) {
            double number = value instanceof Number numeric ? numeric.doubleValue() : Double.parseDouble(String.valueOf(value));
            if (number < min) {
                return min;
            }
            if (number > max) {
                return max;
            }
            return number;
        }
    }

    public static class BooleanValue extends Value<Boolean> {
        public BooleanValue(String path, boolean value) {
            super(path, value);
            set(value);
        }

        @Override
        protected Boolean normalize(Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        }
    }

    public static class IntValue extends Value<Integer> {
        private final int min;
        private final int max;

        public IntValue(String path, int value, int min, int max) {
            super(path, value);
            this.min = min;
            this.max = max;
            set(value);
        }

        @Override
        protected Integer normalize(Object value) {
            int number = value instanceof Number numeric ? numeric.intValue() : Integer.parseInt(String.valueOf(value));
            if (number < min) {
                return min;
            }
            if (number > max) {
                return max;
            }
            return number;
        }
    }
}
