package ttv.migami.jeg.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModParticleTypes;

public record CannonMuzzleFlareOption(
        int color,
        int life,
        float fade,
        int animationSpeed,
        float sizeAdd
) implements ParticleOptions {
    public static final MapCodec<CannonMuzzleFlareOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(CannonMuzzleFlareOption::color),
                    Codec.INT.fieldOf("life").forGetter(CannonMuzzleFlareOption::life),
                    Codec.FLOAT.fieldOf("fade").forGetter(CannonMuzzleFlareOption::fade),
                    Codec.INT.fieldOf("animationSpeed").forGetter(CannonMuzzleFlareOption::animationSpeed),
                    Codec.FLOAT.fieldOf("sizeAdd").forGetter(CannonMuzzleFlareOption::sizeAdd)
            ).apply(builder, CannonMuzzleFlareOption::new));

    public static final StreamCodec<ByteBuf, CannonMuzzleFlareOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            CannonMuzzleFlareOption::color,
            ByteBufCodecs.VAR_INT,
            CannonMuzzleFlareOption::life,
            ByteBufCodecs.FLOAT,
            CannonMuzzleFlareOption::fade,
            ByteBufCodecs.VAR_INT,
            CannonMuzzleFlareOption::animationSpeed,
            ByteBufCodecs.FLOAT,
            CannonMuzzleFlareOption::sizeAdd,
            CannonMuzzleFlareOption::new
    );

    public CannonMuzzleFlareOption(float red, float green, float blue, int life, float fade, int animationSpeed, float sizeAdd) {
        this(Math.round(red * 255.0F) << 16 | Math.round(green * 255.0F) << 8 | Math.round(blue * 255.0F),
                life, fade, animationSpeed, sizeAdd);
    }

    public float red() {
        return (this.color >> 16 & 255) / 255.0F;
    }

    public float green() {
        return (this.color >> 8 & 255) / 255.0F;
    }

    public float blue() {
        return (this.color & 255) / 255.0F;
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticleTypes.CANNON_MUZZLE_FLARE.get();
    }
}
