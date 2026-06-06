package ttv.migami.jeg.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.particle.LaserOption;

public final class LaserParticle extends SingleQuadParticle {
    private final Direction direction;
    private final BlockPos pos;

    private LaserParticle(ClientLevel level, double x, double y, double z, Direction direction, BlockPos pos, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.direction = direction;
        this.pos = pos;
        this.lifetime = 4;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.10F;
        this.setColor(1.0F, 0.0F, 0.0F);
        this.alpha = 0.9F;
        this.setSprite(this.sprite);

        if (level.getBlockState(pos).isAir()) {
            this.remove();
        }
    }

    @Override
    public @NotNull SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT_TERRAIN;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return Brightness.FULL_BRIGHT.pack();
    }

    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTick) {
        this.extractRotatedQuad(renderState, camera, this.direction.getRotation(), partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.getBlockState(this.pos).isAir()) {
            this.remove();
        }
    }

    public static final class Provider implements ParticleProvider<LaserOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                @NotNull LaserOption option,
                @NotNull ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new LaserParticle(level, x, y, z, option.direction(), option.pos(), this.sprites);
        }
    }
}
