package ttv.migami.jeg.client.medal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

final class Medal {
    private final ResourceLocation texture;
    private final Component text;
    private final SoundEvent sound;
    private int ticks;

    Medal(ResourceLocation texture, Component text, SoundEvent sound, int ticks) {
        this.texture = texture;
        this.text = text;
        this.sound = sound;
        this.ticks = ticks;
    }

    ResourceLocation texture() {
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
