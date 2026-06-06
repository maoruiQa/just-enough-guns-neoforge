package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public final class CasingParticle extends SingleQuadParticle {
    private CasingParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            RandomSource random
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites.get(random));
        this.gravity = 0.75F;
        this.friction = 0.999F;
        this.hasPhysics = true;
        this.xd = xSpeed;
        this.yd = ySpeed + this.random.nextFloat() * 0.225F + 0.22F;
        this.zd = zSpeed;
        this.quadSize = 0.35F;
        this.lifetime = (int) (16.0D / (this.random.nextDouble() * 0.8D + 0.2D));
    }

    @Override
    public @NotNull SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float progress = ((float) this.age + scaleFactor) / (float) this.lifetime;
        return this.quadSize * (1.0F - progress * progress);
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                @NotNull SimpleParticleType type,
                @NotNull ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new CasingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, random);
        }
    }
}
