package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class PopcornParticle extends TextureSheetParticle {
    private PopcornParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.gravity = 0.75F;
        this.friction = 0.999F;
        this.xd *= 0.8D;
        this.yd *= 0.8D;
        this.zd *= 0.8D;
        this.yd = this.random.nextFloat() * 0.4F + 0.05F;
        this.quadSize *= (1.0F + this.random.nextFloat()) * 1.5F;
        this.lifetime = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public int getLightColor(float partialTick) {
        int color = super.getLightColor(partialTick);
        int sky = color >> 16 & 255;
        return 240 | sky << 16;
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
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            PopcornParticle particle = new PopcornParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
