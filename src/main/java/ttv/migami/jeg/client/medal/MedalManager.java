package ttv.migami.jeg.client.medal;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModSounds;
import ttv.migami.jeg.network.MedalType;

public final class MedalManager {
    private static final int MEDAL_SIZE = 64;
    private static final int LIFETIME_TICKS = 80;
    private static final long MULTIKILL_WINDOW_MS = 2000L;

    private static final Queue<Medal> PENDING = new ArrayDeque<>();
    private static Medal current;
    private static long lastKillTime;
    private static int multiKillCount;

    private MedalManager() {
    }

    public static void showHeadshot() {
        enqueue(MedalType.COMBAT_HEADSHOT, sound("ui.medal.headshot"));
    }

    public static void showMedal(int ordinal) {
        enqueue(MedalType.byOrdinal(ordinal), sound("ui.medal.generic"));
    }

    public static void showKill() {
        long now = System.currentTimeMillis();
        multiKillCount = now - lastKillTime <= MULTIKILL_WINDOW_MS ? multiKillCount + 1 : 1;
        lastKillTime = now;
        enqueue(killMedalType(), sound("ui.medal.generic"));
    }

    public static void tick() {
        if (current == null) {
            playNext();
            return;
        }
        if (current.tick()) {
            current = null;
            playNext();
        }
    }

    public static void render(GuiGraphics guiGraphics) {
        if (current == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int x = (minecraft.getWindow().getGuiScaledWidth() - MEDAL_SIZE) / 2;
        int y = 20;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, current.opacity());

        guiGraphics.blit(current.texture(), x, y, 0.0F, 0.0F, MEDAL_SIZE, MEDAL_SIZE, MEDAL_SIZE, MEDAL_SIZE);

        int textX = x + (MEDAL_SIZE - minecraft.font.width(current.text())) / 2;
        guiGraphics.drawString(minecraft.font, current.text(), textX, y + MEDAL_SIZE + 4, 0xFFFFFFFF, true);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void enqueue(MedalType type, SoundEvent sound) {
        Medal medal = new Medal(
                Reference.id("textures/gui/medal/" + type.texturePath() + ".png"),
                Component.translatable(type.translationKey()),
                sound,
                LIFETIME_TICKS
        );
        if (current == null) {
            current = medal;
            playSound(medal.sound());
        } else {
            PENDING.add(medal);
        }
    }

    private static void playNext() {
        current = PENDING.poll();
        if (current != null) {
            playSound(current.sound());
        }
    }

    private static MedalType killMedalType() {
        if (multiKillCount == 2) {
            return MedalType.MULTIKILL_DOUBLE_KILL;
        }
        if (multiKillCount == 3) {
            return MedalType.MULTIKILL_TRIPLE_KILL;
        }
        if (multiKillCount == 4) {
            return MedalType.MULTIKILL_QUAD_KILL;
        }
        if (multiKillCount == 5) {
            return MedalType.MULTIKILL_PENTA_KILL;
        }
        if (multiKillCount > 5) {
            return MedalType.MULTIKILL_KILLING_SPREE;
        }
        return MedalType.MULTIKILL_SINGLE_KILL;
    }

    private static SoundEvent sound(String id) {
        var sound = ModSounds.ALL.get(Reference.id(id));
        return sound != null ? sound.get() : null;
    }

    private static void playSound(SoundEvent sound) {
        if (sound != null && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(sound, 1.0F, 1.0F);
        }
    }
}
