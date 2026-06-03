package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class ConfettiParticle extends TextureSheetParticle {
    private static final float[][] COLORS = {
            {1.0F, 0.0F, 0.0F},
            {1.0F, 0.75F, 0.8F},
            {1.0F, 1.0F, 0.0F},
            {0.5F, 1.0F, 0.0F},
            {0.0F, 0.5F, 1.0F},
            {1.0F, 1.0F, 1.0F},
            {1.0F, 0.5F, 0.0F},
            {0.58F, 0.0F, 0.83F},
            {0.0F, 1.0F, 1.0F},
            {1.0F, 0.0F, 1.0F},
            {0.0F, 1.0F, 0.5F},
            {0.0F, 0.0F, 1.0F},
            {1.0F, 0.2F, 0.2F},
            {0.0F, 1.0F, 0.0F},
            {0.94F, 0.9F, 0.55F},
            {1.0F, 0.3F, 0.7F},
            {0.75F, 0.0F, 0.2F},
            {1.0F, 0.85F, 0.0F},
            {0.13F, 0.55F, 0.13F}
    };

    private ConfettiParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.gravity = 0.2F;
        this.friction = 0.999F;
        this.xd *= 0.8D;
        this.yd *= 0.8D;
        this.zd *= 0.8D;
        this.yd = this.random.nextFloat() * 0.4F + 0.05F;
        this.quadSize *= this.random.nextFloat();
        this.lifetime = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
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
            ConfettiParticle particle = new ConfettiParticle(level, x, y, z);
            particle.pickSprite(this.sprites);
            float[] color = COLORS[level.getRandom().nextInt(COLORS.length)];
            particle.setColor(color[0], color[1], color[2]);
            return particle;
        }
    }
}
