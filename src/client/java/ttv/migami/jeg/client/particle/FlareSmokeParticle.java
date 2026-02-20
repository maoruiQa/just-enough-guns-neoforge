package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class FlareSmokeParticle extends ExplodeParticle {
    private FlareSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            float red,
            float green,
            float blue,
            float sizeMultiplier,
            int minLifetime,
            int randomLifetime
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        this.setColor(red, green, blue);
        this.quadSize *= sizeMultiplier;
        this.lifetime = minLifetime + this.random.nextInt(Math.max(1, randomLifetime));
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
    }

    public static final class SmokeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SmokeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new FlareSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites,
                    1.0F, 0.2F, 0.2F, 6.0F, 120, 121);
        }
    }

    public static final class RedProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public RedProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new FlareSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites,
                    1.0F, 0.2F, 0.2F, 5.0F, 80, 81);
        }
    }

    public static final class BlueProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BlueProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new FlareSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites,
                    0.0F, 0.6F, 0.8F, 5.0F, 80, 81);
        }
    }
}
