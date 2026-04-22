package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;

public final class KeyBindings {
    private KeyBindings() {}

    private static boolean initialised;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Reference.id("jeg"));

    public static final KeyMapping RELOAD = new KeyMapping(
            "key.jeg.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        KeyMappingHelper.registerKeyMapping(RELOAD);
    }
}
