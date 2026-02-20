package ttv.migami.jeg.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.effect.FactionOmenEffect;

public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(Registries.MOB_EFFECT, Reference.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> FACTION_OMEN = REGISTER.register(
            "faction_omen",
            () -> new FactionOmenEffect(MobEffectCategory.NEUTRAL, 0xA10E0E)
    );
}

