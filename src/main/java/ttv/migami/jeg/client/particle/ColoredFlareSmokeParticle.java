package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.particle.ColoredFlareOption;

public final class ColoredFlareSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private ColoredFlareSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            ColoredFlareOption option
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.friction = 0.8F;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize *= 21.0F;
        this.sprites = sprites;
        this.lifetime = 1500 + this.random.nextInt(1500);
        this.setSpriteFromAge(sprites);
        this.setColor(option.red(), option.green(), option.blue());
    }

    @Override
    public void tick() {
        super.tick();
        updateAlpha();

        this.yd += 0.002F;
        this.xd += 0.003D;
        this.zd += 0.003D;

        float normalizedAge = (float) this.age / this.lifetime;
        if (normalizedAge > 0.2F) {
            this.xd += (this.random.nextFloat() - 0.5F) * 0.01F;
            this.zd += (this.random.nextFloat() - 0.5F) * 0.01F;
            this.yd += this.random.nextFloat() * 0.005F;
        }

        this.xd *= 0.98F;
        this.zd *= 0.98F;
        this.yd *= 0.98F;
    }

    private void updateAlpha() {
        float normalizedAge = (float) this.age / this.lifetime;
        if (normalizedAge < 0.2F) {
            this.alpha = 1.0F;
        } else {
            this.alpha = 1.0F - (normalizedAge - 0.2F) / 0.8F;
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        float normalizedAge = Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
        int lightValue;
        if (this.age < 30) {
            lightValue = (int) (240.0F - ((float) this.age / 30.0F) * 230.0F);
        } else if (normalizedAge >= 0.2F) {
            lightValue = 200;
        } else {
            lightValue = 10;
        }

        int packedLight = super.getLightColor(partialTick);
        int skyLight = packedLight >> 16 & 255;
        return Math.max(10, lightValue) | skyLight << 16;
    }

    @Override
    public float getQuadSize(float scaleFactor) {
        float normalizedAge = ((float) this.age + scaleFactor) / (float) this.lifetime;
        if (normalizedAge < 0.2F) {
            return this.quadSize * (0.5F + normalizedAge * 5.0F);
        }
        return this.quadSize * (1.5F + (normalizedAge - 0.2F) * 0.8F);
    }

    public static final class Provider implements ParticleProvider<ColoredFlareOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull ColoredFlareOption option, @NotNull ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new ColoredFlareSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, option);
        }
    }
}
