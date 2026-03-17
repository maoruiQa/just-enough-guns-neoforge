package ttv.migami.jeg.config;

public class ModConfigSpec {
    public static class Builder {
        public Builder push(String path) {
            return this;
        }

        public Builder pop() {
            return this;
        }

        public Builder comment(String... comments) {
            return this;
        }

        public DoubleValue defineInRange(String path, double defaultValue, double min, double max) {
            return new DoubleValue(defaultValue);
        }

        public BooleanValue define(String path, boolean defaultValue) {
            return new BooleanValue(defaultValue);
        }

        public IntValue defineInRange(String path, int defaultValue, int min, int max) {
            return new IntValue(defaultValue);
        }

        public ModConfigSpec build() {
            return new ModConfigSpec();
        }
    }

    public static class DoubleValue {
        private final double value;

        public DoubleValue(double value) {
            this.value = value;
        }

        public double get() {
            return value;
        }
    }

    public static class BooleanValue {
        private final boolean value;

        public BooleanValue(boolean value) {
            this.value = value;
        }

        public boolean get() {
            return value;
        }
    }

    public static class IntValue {
        private final int value;

        public IntValue(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }
    }
}
