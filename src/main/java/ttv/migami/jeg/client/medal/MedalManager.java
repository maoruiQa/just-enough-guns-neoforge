package ttv.migami.jeg.client.medal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModSounds;

public final class MedalManager {
    private static final ResourceLocation HEADSHOT_TEXTURE = Reference.id("textures/gui/medal/combat_headshot.png");
    private static final Component HEADSHOT_TEXT = Component.translatable("medal.jeg.combat_headshot");
    private static final int MEDAL_SIZE = 64;
    private static final int LIFETIME_TICKS = 180;

    private static int headshotTicks;

    private MedalManager() {
    }

    public static void showHeadshot() {
        headshotTicks = LIFETIME_TICKS;
        var sound = ModSounds.ALL.get(Reference.id("ui.medal.headshot"));
        if (sound != null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(sound.get(), 1.0F, 1.0F);
        }
    }

    public static void tick() {
        if (headshotTicks > 0) {
            headshotTicks--;
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        if (headshotTicks <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int x = (minecraft.getWindow().getGuiScaledWidth() - MEDAL_SIZE) / 2;
        int y = 20;
        guiGraphics.blit(HEADSHOT_TEXTURE, x, y, 0.0F, 0.0F, MEDAL_SIZE, MEDAL_SIZE, MEDAL_SIZE, MEDAL_SIZE);

        int textX = x + (MEDAL_SIZE - minecraft.font.width(HEADSHOT_TEXT)) / 2;
        guiGraphics.drawString(minecraft.font, HEADSHOT_TEXT, textX, y + MEDAL_SIZE + 4, 0xFFFFFFFF, true);
    }
}
