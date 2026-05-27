package ttv.migami.jeg.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import ttv.migami.jeg.particle.CannonMuzzleFlareOption;

public final class CannonMuzzleFlareParticle extends ExplodeParticle {
    private final SpriteSet sprites;
    private final float fade;
    private final int animationSpeed;
    private final float sizeAdd;

    private CannonMuzzleFlareParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            SpriteSet sprites,
            CannonMuzzleFlareOption option
    ) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
        this.sprites = sprites;
        this.setSize(0.35F, 0.35F);
        this.quadSize *= 11.0F;
        this.lifetime = Math.max(1, option.life() + this.random.nextInt(1));
        this.gravity = -0.05F;
        this.hasPhysics = false;
        this.xd = xSpeed * 0.6D;
        this.yd = ySpeed * 0.6D;
        this.zd = zSpeed * 0.6D;
        this.setSpriteFromAge(sprites);
        this.rCol = option.red();
        this.gCol = option.green();
        this.bCol = option.blue();
        this.roll = this.random.nextFloat() * ((float) Math.PI * 0.01F);
        this.fade = option.fade();
        this.animationSpeed = Math.max(1, option.animationSpeed());
        this.sizeAdd = option.sizeAdd();
    }

    @Override
    protected int getLightCoords(float partialTick) {
        return 15728880;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSprite(this.sprites.get(Mth.clamp((this.age / this.animationSpeed) % 12 + 1, 0, 12), 12));
        }
        this.quadSize += this.sizeAdd;
        this.alpha *= this.fade;
        this.rCol *= 0.93F;
        this.gCol *= 0.93F;
        this.bCol *= 0.93F;
    }

    public static final class Provider implements ParticleProvider<CannonMuzzleFlareOption> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                CannonMuzzleFlareOption type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed,
                RandomSource random
        ) {
            return new CannonMuzzleFlareParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, type);
        }
    }
}
