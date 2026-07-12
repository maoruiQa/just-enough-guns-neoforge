package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ServerConfigScreenEvents {
    private ServerConfigScreenEvents() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)
                || !pauseScreen.showsPauseMenu()
                || minecraft.player == null) {
            return;
        }
        event.addListener(Button.builder(
                Component.translatable("gui.jegn.config.menu_button"),
                button -> {
                    button.active = false;
                    button.setMessage(Component.translatable("gui.jegn.config.status.loading"));
                    ServerConfigClient.requestOpen(event.getScreen());
                }
        ).bounds(event.getScreen().width / 2 - 102, 8, 204, 20).build());
    }
}
