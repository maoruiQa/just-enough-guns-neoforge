package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.effect.FactionOmenEffect;
import ttv.migami.jeg.effect.IncurableEffect;
import ttv.migami.jeg.effect.KillEffectEffect;
import ttv.migami.jeg.effect.SmokedEffect;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(Registries.MOB_EFFECT, Reference.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FACTION_OMEN = REGISTER.register(
            "faction_omen",
            () -> new FactionOmenEffect(MobEffectCategory.NEUTRAL, 0xA10E0E)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLINDED = REGISTER.register(
            "blinded",
            () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0xF7F0BA)
    );

    public static final DeferredHolder<MobEffect, MobEffect> DEAFENED = REGISTER.register(
            "deafened",
            () -> new IncurableEffect(MobEffectCategory.HARMFUL, 0xD5D7DE)
    );

    public static final DeferredHolder<MobEffect, MobEffect> SMOKED = REGISTER.register(
            "smoked",
            () -> new SmokedEffect(MobEffectCategory.HARMFUL, 0x666666)
    );

    public static final DeferredHolder<MobEffect, MobEffect> POPPED = REGISTER.register(
            "popped",
            () -> new KillEffectEffect(MobEffectCategory.HARMFUL, 0xFFE8A3)
    );

    public static final DeferredHolder<MobEffect, MobEffect> TRICKSHOTTED = REGISTER.register(
            "trickshotted",
            () -> new KillEffectEffect(MobEffectCategory.HARMFUL, 0xFFDF4D)
    );
}
