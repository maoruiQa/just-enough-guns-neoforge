package ttv.migami.jeg.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.init.ModEffects;
import ttv.migami.jeg.init.ModParticleTypes;
import ttv.migami.jeg.init.ModSounds;

public final class KillEffectEffect extends IncurableEffect {
    public KillEffectEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.POPPED.get()))) {
            level.sendParticles(
                    ModParticleTypes.POPCORN.get(),
                    entity.getX(),
                    entity.getY() + entity.getEyeHeight(),
                    entity.getZ(),
                    1,
                    entity.getBbWidth() / 1.5D,
                    entity.getBbHeight() - entity.getEyeHeight(),
                    entity.getBbWidth() / 1.5D,
                    0.0D
            );
            if (level.getRandom().nextBoolean()) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 5.0F, 0.5F + level.getRandom().nextFloat());
            }
        }
        if (entity.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.TRICKSHOTTED.get()))) {
            level.sendParticles(
                    ModParticleTypes.HIT_MARKER.get(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    1,
                    entity.getBbWidth() / 1.2D,
                    entity.getBbHeight(),
                    entity.getBbWidth() / 1.2D,
                    0.0D
            );
            play(level, entity, "entity.bullet.hit", 1.0F, 1.0F);
            if (level.getRandom().nextFloat() < 0.2F) {
                if (level.getRandom().nextFloat() < 0.1F) {
                    play(level, entity, "item.goose", 5.0F, 1.0F);
                } else {
                    play(level, entity, "item.kill_effect.air_horn", 5.0F, 1.0F);
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private static void play(ServerLevel level, LivingEntity entity, String soundPath, float volume, float pitch) {
        DeferredHolder<SoundEvent, SoundEvent> holder = ModSounds.ALL.get(Reference.id(soundPath));
        if (holder != null) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), holder.get(), SoundSource.PLAYERS, volume, pitch);
        }
    }
}
