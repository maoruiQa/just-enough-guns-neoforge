
package ttv.migami.jeg;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        CLIENT_SPEC = new ModConfigSpec.Builder().build();
        SERVER_SPEC = new ModConfigSpec.Builder().build();
    }

    private Config() {}
}
