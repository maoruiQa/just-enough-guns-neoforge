package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {
    private static final String CATEGORY = "key.categories." + Reference.MOD_ID;

    public static final KeyMapping RELOAD = new KeyMapping(
            "key.jeg.reload",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_FREE_LOOK = new KeyMapping(
            "key.jeg.vehicle_free_look",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_SWITCH_WEAPON = new KeyMapping(
            "key.jeg.vehicle_switch_weapon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_DEPLOY_DECOY = new KeyMapping(
            "key.jeg.vehicle_deploy_decoy",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_BRAKE_DESCEND = new KeyMapping(
            "key.jeg.vehicle_brake_descend",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_CHANGE_SEAT = new KeyMapping(
            "key.jeg.vehicle_change_seat",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    private KeyBindings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(RELOAD);
        event.register(VEHICLE_FREE_LOOK);
        event.register(VEHICLE_SWITCH_WEAPON);
        event.register(VEHICLE_DEPLOY_DECOY);
        event.register(VEHICLE_BRAKE_DESCEND);
        event.register(VEHICLE_CHANGE_SEAT);
    }
}
