package ttv.migami.jeg.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import ttv.migami.jeg.Reference;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENTITY_LASER =
            REGISTER.register("entity_laser", FabricParticleTypes::simple);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_EXPLOSION =
            REGISTER.register("big_explosion", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_EXPLOSION =
            REGISTER.register("small_explosion", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE =
            REGISTER.register("smoke", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE =
            REGISTER.register("fire", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLARE_SMOKE =
            REGISTER.register("flare_smoke", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLARE =
            REGISTER.register("flare", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUE_FLARE =
            REGISTER.register("flare_blue", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAME =
            REGISTER.register("flame", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUE_FLAME =
            REGISTER.register("blue_flame", FabricParticleTypes::simple);
}
