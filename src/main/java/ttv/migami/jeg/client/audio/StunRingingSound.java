package ttv.migami.jeg.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModSounds;

public final class StunRingingSound extends AbstractTickableSoundInstance {
    private static final float MAX_VOLUME = 0.85F;
    private static final int FADE_TICKS = 80;

    public StunRingingSound() {
        super(ModSounds.ALL.get(Reference.id("entity.stun_grenade.ring")).get(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.attenuation = Attenuation.NONE;
        this.delay = 0;
    }

    @Override
    public void tick() {
        Player player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) {
            this.stop();
            return;
        }

        MobEffectInstance effect = player.getEffect(ModEffects.DEAFENED);
        if (effect == null) {
            this.stop();
            return;
        }

        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        float strength = Math.min(1.0F, effect.getDuration() / (float) FADE_TICKS);
        this.volume = MAX_VOLUME * strength;
        this.pitch = 0.95F;
    }
}
