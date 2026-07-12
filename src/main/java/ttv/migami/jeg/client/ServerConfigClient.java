package ttv.migami.jeg.client;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ttv.migami.jeg.client.screen.ServerConfigScreen;
import ttv.migami.jeg.network.ApplyServerConfigPayload;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.network.ServerConfigStatePayload;

public final class ServerConfigClient {
    private static Screen pendingParent;

    private ServerConfigClient() {}

    public static void requestOpen(Screen parent) {
        pendingParent = parent;
        NetworkHandler.sendOpenServerConfig();
    }

    public static void apply(Map<String, String> changes) {
        NetworkHandler.sendServerConfigChanges(new ApplyServerConfigPayload(changes));
    }

    public static void handle(ServerConfigStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.status() == ServerConfigStatePayload.Status.DENIED) {
            Screen parent = minecraft.screen instanceof ServerConfigScreen screen ? screen.parent() : pendingParent;
            minecraft.setScreen(new AlertScreen(
                    () -> minecraft.setScreen(parent),
                    Component.translatable("gui.jegn.config.permission.title"),
                    Component.translatable("gui.jegn.config.permission.message")
            ));
            return;
        }
        if (payload.status() == ServerConfigStatePayload.Status.OPEN) {
            Screen parent = pendingParent;
            pendingParent = null;
            minecraft.setScreen(new ServerConfigScreen(parent, payload.values()));
            return;
        }
        if (minecraft.screen instanceof ServerConfigScreen screen) {
            if (payload.status() == ServerConfigStatePayload.Status.APPLIED) {
                screen.applyAuthoritativeValues(payload.values(), payload.changedCount());
            } else if (payload.status() == ServerConfigStatePayload.Status.INVALID) {
                screen.showInvalidValues(payload.invalidKeys());
            }
        }
    }
}
