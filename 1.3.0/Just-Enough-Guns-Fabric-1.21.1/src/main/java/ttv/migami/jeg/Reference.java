
package ttv.migami.jeg;

import net.minecraft.resources.ResourceLocation;

public final class Reference {
    public static final String MOD_ID = "jeg";

    private Reference() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
