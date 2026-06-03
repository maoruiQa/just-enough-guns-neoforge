package ttv.migami.jeg.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import ttv.migami.jeg.Reference;
import ttv.migami.jeg.particle.CannonMuzzleFlareOption;

public class ModParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Reference.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENTITY_LASER =
            REGISTER.register("entity_laser", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_EXPLOSION =
            REGISTER.register("big_explosion", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMALL_EXPLOSION =
            REGISTER.register("small_explosion", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE =
            REGISTER.register("smoke", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE =
            REGISTER.register("fire", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLARE_SMOKE =
            REGISTER.register("flare_smoke", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLARE =
            REGISTER.register("flare", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUE_FLARE =
            REGISTER.register("flare_blue", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLAME =
            REGISTER.register("flame", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLUE_FLAME =
            REGISTER.register("blue_flame", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUN_MUZZLE_FLASH =
            REGISTER.register("gun_muzzle_flash", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_RING =
            REGISTER.register("sonic_ring", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SONIC_RING =
            REGISTER.register("big_sonic_ring", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, ParticleType<CannonMuzzleFlareOption>> CANNON_MUZZLE_FLARE =
            REGISTER.register("cannon_muzzle_flare", () -> createOptions(CannonMuzzleFlareOption.CODEC, true, CannonMuzzleFlareOption.STREAM_CODEC));

    public static <T extends ParticleOptions> ParticleType<T> createOptions(MapCodec<T> codec, boolean overrideLimiter, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return new ParticleType<>(overrideLimiter) {
            @Override
            public @NotNull MapCodec<T> codec() {
                return codec;
            }

            @Override
            public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }
}
