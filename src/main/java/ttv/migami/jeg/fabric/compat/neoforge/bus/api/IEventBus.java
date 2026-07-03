package ttv.migami.jeg.fabric.compat.neoforge.bus.api;

import java.util.function.Consumer;

public interface IEventBus {
    default void register(Object listener) {
    }

    default void register(Class<?> listenerClass) {
    }

    default <T> void addListener(Consumer<T> consumer) {
    }
}
