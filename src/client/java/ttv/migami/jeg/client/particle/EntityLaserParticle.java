package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.NotNull;

public final class EntityLaserParticle extends TextureSheetParticle {
    private EntityLaserParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        float color = this.random.nextFloat() * 0.1F + 0.2F;
        this.rCol = color;
        this.gCol = color;
        this.bCol = color;
        this.setSize(0.02F, 0.02F);
        this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
        this.xd *= 0.02D;
        this.yd *= 0.02D;
        this.zd *= 0.02D;
        this.hasPhysics = false;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
            EntityLaserParticle particle = new EntityLaserParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            particle.setColor(1.0F, 1.0F, 1.0F);
            particle.setLifetime(1 + level.getRandom().nextInt(4));
            return particle;
        }
    }
}
