package ttv.migami.jeg.init;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredHolder;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import ttv.migami.jeg.Reference;
import ttv.migami.jeg.particle.CannonMuzzleFlareOption;
import ttv.migami.jeg.particle.ColoredFlareOption;
import ttv.migami.jeg.particle.CustomSmokeOption;
import ttv.migami.jeg.particle.LaserOption;

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
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GUN_MUZZLE_FLASH =
            REGISTER.register("gun_muzzle_flash", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CASING_PARTICLE =
            REGISTER.register("casing", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHELL_PARTICLE =
            REGISTER.register("shell", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPECTRE_CASING_PARTICLE =
            REGISTER.register("spectre_casing", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SONIC_RING =
            REGISTER.register("sonic_ring", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BIG_SONIC_RING =
            REGISTER.register("big_sonic_ring", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CONFETTI =
            REGISTER.register("confetti", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HIT_MARKER =
            REGISTER.register("hit_marker", FabricParticleTypes::simple);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> POPCORN =
            REGISTER.register("popcorn", FabricParticleTypes::simple);

    public static final DeferredHolder<ParticleType<?>, ParticleType<ColoredFlareOption>> COLORED_FLARE_SMOKE =
            REGISTER.register("colored_flare_smoke", () -> createOptions(ColoredFlareOption.CODEC, true, ColoredFlareOption.STREAM_CODEC));
    public static final DeferredHolder<ParticleType<?>, ParticleType<CustomSmokeOption>> CUSTOM_SMOKE =
            REGISTER.register("custom_smoke", () -> createOptions(CustomSmokeOption.CODEC, true, CustomSmokeOption.STREAM_CODEC));
    public static final DeferredHolder<ParticleType<?>, ParticleType<LaserOption>> LASER =
            REGISTER.register("laser", () -> createOptions(LaserOption.CODEC, false, LaserOption.STREAM_CODEC));
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
