package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class GunMuzzleFlashParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private GunMuzzleFlashParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 2;
        this.quadSize = 1.15F + this.random.nextFloat() * 0.25F;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = xSpeed * 0.1D;
        this.yd = ySpeed * 0.1D;
        this.zd = zSpeed * 0.1D;
        this.setSpriteFromAge(sprites);
        this.roll = this.random.nextFloat() * ((float) Math.PI * 2.0F);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            this.alpha *= 0.55F;
            this.quadSize *= 0.92F;
        }
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
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
                double zSpeed
        ) {
            return new GunMuzzleFlashParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
