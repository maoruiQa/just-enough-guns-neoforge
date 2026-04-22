package ttv.migami.jeg.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class HudMessageHelper {
    private HudMessageHelper() {}

    public static void showActionBar(Player player, Component message) {
        player.displayClientMessage(message, true);
    }
}
