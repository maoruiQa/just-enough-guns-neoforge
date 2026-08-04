package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.particle.CustomSmokeOption;

/** Strict Superb Warfare CustomSmokeParticle port. */
public final class CustomSmokeParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    private CustomSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double vx,
            double vy,
            double vz,
            SpriteSet spriteSet,
            float rCol,
            float gCol,
            float bCol
    ) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.setSize(0.4F, 0.4F);
        this.quadSize *= 10.0F;
        this.lifetime = this.random.nextInt(200) + 600;
        this.gravity = 0.001F;
        this.hasPhysics = true;
        this.xd = vx * 0.5D;
        this.yd = vy * 0.5D;
        this.zd = vz * 0.5D;
        this.setSpriteFromAge(spriteSet);
        this.rCol = rCol;
        this.gCol = gCol;
        this.bCol = bCol;
    }

    public static final class Provider implements ParticleProvider<CustomSmokeOption> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(
                CustomSmokeOption type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new CustomSmokeParticle(
                    level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, type.getRed(), type.getGreen(), type.getBlue());
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.spriteSet.get(Math.min((this.age / 8) + 1, 8), 8));
        }
        if (this.age++ < this.lifetime && !(this.alpha <= 0.0F)) {
            if (this.age >= this.lifetime - 60 && this.alpha > 0.01F) {
                this.alpha -= 0.015F;
            }
        } else {
            this.remove();
        }
    }
}
