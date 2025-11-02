package ttv.migami.jeg.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import ttv.migami.jeg.Reference;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENTITY_LASER =
            REGISTER.register("entity_laser", () -> new SimpleParticleType(false));
}