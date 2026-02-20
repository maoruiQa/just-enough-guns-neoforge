
package ttv.migami.jeg;

import net.minecraft.resources.Identifier;

public final class Reference {
    public static final String MOD_ID = "jeg";

    private Reference() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
