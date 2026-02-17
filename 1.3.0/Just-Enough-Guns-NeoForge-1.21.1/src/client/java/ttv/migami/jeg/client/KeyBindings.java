package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class KeyBindings {
    private KeyBindings() {}

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Reference.id("jeg"));

    public static final KeyMapping RELOAD = new KeyMapping(
            "key.jeg.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
    }
}
