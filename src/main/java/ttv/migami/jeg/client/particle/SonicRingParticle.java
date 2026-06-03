package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class SonicRingParticle extends RisingParticle {
    private SonicRingParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.hasPhysics = false;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age <= 10) {
            this.setAlpha(0.75F * (1.0F - (this.age / 10.0F)));
        }
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float progress = ((float) this.age + scaleFactor) / (float) this.lifetime;
        return this.quadSize * (1.0F + progress * progress);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 240;
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
                double zSpeed
        ) {
            SonicRingParticle particle = new SonicRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            particle.setAlpha(0.75F);
            particle.scale(3.0F);
            return particle;
        }
    }

    public static final class BigProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BigProvider(SpriteSet sprites) {
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
                double zSpeed
        ) {
            SonicRingParticle particle = new SonicRingParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            particle.setAlpha(0.75F);
            particle.scale(17.0F);
            return particle;
        }
    }
}
