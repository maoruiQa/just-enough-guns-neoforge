package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import ttv.migami.jeg.entity.BulletEntity;

public final class BulletRenderer extends EntityRenderer<BulletEntity, BulletRenderer.State> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/particle/generic_0.png");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(State state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        Vec3 motion = state.motion;
        if (motion == null || motion.lengthSqr() < 1.0E-6) {
            return;
        }

        poseStack.pushPose();

        // Render bullet trail as a quad billboard
        Vec3 normalized = motion.normalize();
        double trailLength = 0.3 * state.trailLength;

        Vec3 start = new Vec3(state.x, state.y, state.z);
        Vec3 end = start.subtract(normalized.scale(trailLength));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix4f = poseStack.last().pose();

        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();

        // Calculate perpendicular vector for quad width
        Vec3 cameraDir = start.subtract(camera).normalize();
        Vec3 perpendicular = normalized.cross(cameraDir).normalize().scale(0.05);

        float red = state.red;
        float green = state.green;
        float blue = state.blue;
        float alpha = state.alpha * 0.8F;

        // Draw quad (4 vertices forming a rectangle)
        Vec3 p1 = start.add(perpendicular);
        Vec3 p2 = start.subtract(perpendicular);
        Vec3 p3 = end.subtract(perpendicular);
        Vec3 p4 = end.add(perpendicular);

        drawVertex(consumer, matrix4f, camera, p1, red, green, blue, alpha);
        drawVertex(consumer, matrix4f, camera, p2, red, green, blue, alpha);
        drawVertex(consumer, matrix4f, camera, p3, red, green, blue, alpha * 0.3F);
        drawVertex(consumer, matrix4f, camera, p4, red, green, blue, alpha * 0.3F);

        poseStack.popPose();
        super.render(state, poseStack, bufferSource, packedLight);
    }

    private void drawVertex(VertexConsumer consumer, Matrix4f pose, Vec3 camera, Vec3 pos, float r, float g, float b, float a) {
        consumer.addVertex(pose, (float)(pos.x - camera.x()), (float)(pos.y - camera.y()), (float)(pos.z - camera.z()))
                .setColor(r, g, b, a)
                .setUv(0, 0)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.partialTick = partialTick;
        state.motion = entity.getDeltaMovement();
        state.size = entity.getProjectileSize();
        state.trailLength = entity.getTrailLengthMultiplier();
        int color = entity.getTrailColor();
        float alpha = ((color >> 24) & 0xFF) / 255.0F;
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }
        state.alpha = alpha;
        state.red = ((color >> 16) & 0xFF) / 255.0F;
        state.green = ((color >> 8) & 0xFF) / 255.0F;
        state.blue = (color & 0xFF) / 255.0F;
    }

    public static final class State extends EntityRenderState {
        Vec3 motion = Vec3.ZERO;
        float size;
        float trailLength;
        float red;
        float green;
        float blue;
        float alpha = 1.0F;
    }
}
