package ttv.migami.jeg.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ttv.migami.jeg.particle.LaserOption;

public final class LaserParticle extends TextureSheetParticle {
    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    private LaserParticle(ClientLevel level, double x, double y, double z, Direction direction, BlockPos pos) {
        super(level, x, y, z);
        this.setSprite(this.getSprite(pos));
        this.direction = direction;
        this.pos = pos;
        this.lifetime = 0;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.05F;

        if (level.getBlockState(pos).isAir()) {
            this.remove();
        }

        this.rCol = 1.0F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;
        this.alpha = 0.9F;
    }

    @Override
    public int getLightColor(float partialTick) {
        int light = super.getLightColor(partialTick);
        int sky = light >> 16 & 255;
        return 240 | sky << 16;
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    private TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level != null) {
            BlockState state = level.getBlockState(pos);
            return minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
        }
        return minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        this.remove();
        if (this.level.getBlockState(this.pos).isAir()) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 view = renderInfo.getPosition();
        float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternionf quaternion = this.direction.getRotation();
        Vector3f[] points = new Vector3f[] {
                new Vector3f(-1.0F, 0.0F, -1.0F),
                new Vector3f(-1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, 1.0F),
                new Vector3f(1.0F, 0.0F, -1.0F)
        };
        float scale = this.getQuadSize(partialTicks);

        for (Vector3f point : points) {
            point.rotate(quaternion);
            point.mul(scale);
            point.add(particleX, particleY, particleZ);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        vertex(buffer, points[0], u1, v1, light);
        vertex(buffer, points[1], u1, v0, light);
        vertex(buffer, points[2], u0, v0, light);
        vertex(buffer, points[3], u0, v1, light);
    }

    private void vertex(VertexConsumer buffer, Vector3f point, float u, float v, int light) {
        buffer.addVertex(point.x(), point.y(), point.z())
                .setUv(u, v)
                .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
                .setLight(light);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static final class Provider implements ParticleProvider<LaserOption> {
        public Provider(SpriteSet sprites) {
        }

        @Override
        public Particle createParticle(
                @NotNull LaserOption option,
                @NotNull ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new LaserParticle(level, x, y, z, option.direction(), option.pos());
        }
    }
}
