package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ttv.migami.jeg.network.MagazineModeAckPayload;
import ttv.migami.jeg.network.MagazineModePayload;

public final class MagazineModeClient {
    private static MagazineModePayload pending;
    private static MagazineModePayload shown;
    private static Object connection;
    private static Screen popup;
    private static Screen parent;

    private MagazineModeClient() {}

    public static void handle(MagazineModePayload payload) {
        pending = payload;
        connection = Minecraft.getInstance().getConnection();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (connection != minecraft.getConnection()) {
            pending = shown = null;
            popup = parent = null;
            connection = null;
            return;
        }
        if (pending == null || minecraft.player == null || minecraft.level == null) return;
        Screen current = minecraft.screen;
        if (current instanceof ReceivingLevelScreen || current instanceof LevelLoadingScreen) return;
        if (current == popup && shown == pending) return;
        if (current != popup) parent = current;
        MagazineModePayload payload = pending;
        shown = payload;
        Runnable confirm = () -> {
            if (pending != payload || connection != minecraft.getConnection()) return;
            ttv.migami.jeg.network.ClientNetworkHandler.sendMagazineModeAck(new MagazineModeAckPayload(payload.revision()));
            pending = shown = null;
            minecraft.setScreen(parent);
            popup = parent = null;
        };
        Component body = Component.translatable(payload.changed() ? "gui.jeg.magazine_mode.changed" : "gui.jeg.magazine_mode.first")
                .append("\n\n").append(Component.translatable(payload.magazineFeed() ? "gui.jeg.magazine_mode.magazine" : "gui.jeg.magazine_mode.direct"))
                .append("\n\n").append(Component.translatable("gui.jeg.magazine_mode.special"));
        popup = new AlertScreen(confirm, Component.translatable("gui.jeg.magazine_mode.title"), body,
                Component.translatable("gui.jeg.magazine_mode.confirm"), true) {
            @Override public void onClose() { confirm.run(); }
            @Override public boolean isPauseScreen() { return false; }
        };
        minecraft.setScreen(popup);
    }
}
