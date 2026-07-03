package ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries;

import java.util.function.Supplier;

public class DeferredHolder<R, T extends R> implements Supplier<T> {
    private final Supplier<T> supplier;
    private T cached;

    public DeferredHolder(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (cached == null) {
            cached = supplier.get();
        }
        return cached;
    }
}
