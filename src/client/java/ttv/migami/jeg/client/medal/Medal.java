package ttv.migami.jeg.client.medal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

final class Medal {
    private final Identifier texture;
    private final Component text;
    private final SoundEvent sound;
    private int ticks;

    Medal(Identifier texture, Component text, SoundEvent sound, int ticks) {
        this.texture = texture;
        this.text = text;
        this.sound = sound;
        this.ticks = ticks;
    }

    Identifier texture() {
        return texture;
    }

    Component text() {
        return text;
    }

    SoundEvent sound() {
        return sound;
    }

    float opacity() {
        return ticks < 20 ? Math.max(0.0F, ticks / 20.0F) : 1.0F;
    }

    boolean tick() {
        ticks--;
        return ticks <= 0;
    }
}
