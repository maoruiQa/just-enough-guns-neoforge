package ttv.migami.jeg.util;

import java.util.function.Supplier;

public final class ClientOnly {
    private static final boolean PRESENT;

    static {
        boolean client = false;
        try {
            Class.forName("net.minecraft.client.Minecraft");
            client = true;
        } catch (ClassNotFoundException ignored) {
        }
        PRESENT = client;
    }

    private ClientOnly() {
    }

    public static boolean isClient() {
        return PRESENT;
    }

    public static void run(Runnable action) {
        if (PRESENT) {
            action.run();
        }
    }

    public static <T> T call(Supplier<T> supplier, T def) {
        return PRESENT ? supplier.get() : def;
    }
}
