package ttv.migami.jeg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;

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
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_SWITCH_WEAPON = new KeyMapping(
            "key.jeg.vehicle_switch_weapon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_PREVIOUS_WEAPON = new KeyMapping(
            "key.jeg.vehicle_previous_weapon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_DEPLOY_DECOY = new KeyMapping(
            "key.jeg.vehicle_deploy_decoy",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_SEEK = new KeyMapping(
            "key.jeg.vehicle_seek",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
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
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_DISMOUNT = new KeyMapping(
            "key.jeg.vehicle_dismount",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );

    public static final KeyMapping VEHICLE_PLAYER_INVENTORY = new KeyMapping(
            "key.jeg.vehicle_player_inventory",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    private static boolean registered;

    private KeyBindings() {}

    public static void init() {
        if (registered) {
            return;
        }
        registered = true;
        KeyBindingHelper.registerKeyBinding(RELOAD);
        KeyBindingHelper.registerKeyBinding(VEHICLE_FREE_LOOK);
        KeyBindingHelper.registerKeyBinding(VEHICLE_SWITCH_WEAPON);
        KeyBindingHelper.registerKeyBinding(VEHICLE_PREVIOUS_WEAPON);
        KeyBindingHelper.registerKeyBinding(VEHICLE_DEPLOY_DECOY);
        KeyBindingHelper.registerKeyBinding(VEHICLE_SEEK);
        KeyBindingHelper.registerKeyBinding(VEHICLE_BRAKE_DESCEND);
        KeyBindingHelper.registerKeyBinding(VEHICLE_CHANGE_SEAT);
        KeyBindingHelper.registerKeyBinding(VEHICLE_DISMOUNT);
        KeyBindingHelper.registerKeyBinding(VEHICLE_PLAYER_INVENTORY);
    }
}
