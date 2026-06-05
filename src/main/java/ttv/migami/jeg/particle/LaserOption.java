package ttv.migami.jeg.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModParticleTypes;

public record LaserOption(Direction direction, BlockPos pos) implements ParticleOptions {
    public static final MapCodec<LaserOption> CODEC = RecordCodecBuilder.mapCodec(builder ->
            builder.group(
                    Codec.INT.fieldOf("dir").forGetter(option -> option.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong())
            ).apply(builder, LaserOption::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LaserOption> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeEnum(option.direction);
                buf.writeBlockPos(option.pos);
            },
            buf -> new LaserOption(buf.readEnum(Direction.class), buf.readBlockPos())
    );

    public LaserOption(int direction, long pos) {
        this(Direction.values()[direction], BlockPos.of(pos));
    }

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticleTypes.LASER.get();
    }
}
