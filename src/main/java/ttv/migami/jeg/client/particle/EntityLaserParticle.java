package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public final class EntityLaserParticle extends SingleQuadParticle {
    private EntityLaserParticle(
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
        this.setSize(0.02F, 0.02F);
        this.quadSize *= random.nextFloat() * 0.6F + 0.5F;
        this.xd *= 0.02D;
        this.yd *= 0.02D;
        this.zd *= 0.02D;
        this.hasPhysics = false;
        this.setColor(1.0F, 1.0F, 1.0F);
        this.lifetime = 1 + random.nextInt(4);
    }

    @Override
    public @NotNull SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partialTick) {
        return Brightness.FULL_BRIGHT.pack();
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
            return new EntityLaserParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, random);
        }
    }
}
