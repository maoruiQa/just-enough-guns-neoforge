package ttv.migami.jeg.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class HudMessageHelper {
    private HudMessageHelper() {}

    public static void showActionBar(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message, true);
            return;
        }
        player.sendSystemMessage(message);
    }
}
