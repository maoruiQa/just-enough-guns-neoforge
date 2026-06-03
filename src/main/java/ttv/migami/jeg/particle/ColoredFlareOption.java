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

public record ColoredFlareOption(int color) implements ParticleOptions {
    public static final MapCodec<ColoredFlareOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("color").forGetter(ColoredFlareOption::color)
            ).apply(builder, ColoredFlareOption::new));

    public static final StreamCodec<ByteBuf, ColoredFlareOption> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ColoredFlareOption::color,
            ColoredFlareOption::new
    );

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
        return ModParticleTypes.COLORED_FLARE_SMOKE.get();
    }
}
