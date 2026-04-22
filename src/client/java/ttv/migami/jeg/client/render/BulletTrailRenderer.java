package ttv.migami.jeg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import ttv.migami.jeg.network.BulletTrailPayload;

/**
 * 1.20.x-style bullet trail renderer.
 * Trails are maintained per-bullet and rendered as textured energy-swirl geometry.
 */
public final class BulletTrailRenderer {
    private static final Identifier TRAIL_TEXTURE = Identifier.fromNamespaceAndPath("jeg", "textures/misc/bullet_trail.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final Map<Integer, TrailState> TRAILS = new HashMap<>();
    private static final AtomicInteger SYNTHETIC_IDS = new AtomicInteger(-1);
    private static long lastRenderFrame = -1L;

    private BulletTrailRenderer() {}

    public static void upsertLegacyTrail(BulletTrailPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level != null ? mc.level.getGameTime() : 0L;
        int payloadColor = payload.color();
        // Preserve legacy yellow fallback for entries that arrive as plain white.
        if (payloadColor == 0xFFFFFF || payloadColor == 0xFFFFFFFF) {
            payloadColor = 0xFFFF00;
        }
        int count = Math.min(
                payload.entityIds().length,
                Math.min(payload.positions().length, payload.motions().length)
        );
        for (int i = 0; i < count; i++) {
            int entityId = payload.entityIds()[i];
            TrailState state = TRAILS.get(entityId);
            if (state == null) {
                state = new TrailState(entityId);
                TRAILS.put(entityId, state);
            }

            state.position = payload.positions()[i];
            state.motion = payload.motions()[i];
            state.color = payloadColor;
            state.size = payload.size();
            state.maxAge = Math.max(2, payload.life());
            state.gravity = payload.gravity();
            state.shooterId = payload.shooterId();
            state.trailVisible = payload.trailVisible();
            state.lastUpdateTick = gameTime;
            state.updateYawPitch();
            state.age = 0;
        }
    }

    /**
     * Compatibility fallback for local-only trail calls.
     */
    public static void addInstantTrail(Vec3 start, Vec3 end, int color, float size) {
        Vec3 motion = end.subtract(start);
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        int id = SYNTHETIC_IDS.getAndDecrement();
        TrailState state = new TrailState(id);
        state.position = start;
        state.motion = motion;
        state.color = color;
        state.size = size;
        state.maxAge = 3;
        state.gravity = 0.0D;
        state.shooterId = -1;
        state.trailVisible = true;
        state.lastUpdateTick = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        state.updateYawPitch();
        TRAILS.put(id, state);
    }

    public static void updateDynamicTrail(int entityId, Vec3 currentPos, int color, float size) {
        TrailState state = TRAILS.get(entityId);
        if (state == null) {
            return;
        }
        state.position = currentPos;
        state.color = color;
        state.size = size;
        state.lastUpdateTick = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : state.lastUpdateTick;
    }

    public static void removeDynamicTrail(int entityId) {
        TRAILS.remove(entityId);
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            TRAILS.clear();
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        Iterator<TrailState> iterator = TRAILS.values().iterator();
        while (iterator.hasNext()) {
            TrailState trail = iterator.next();
            trail.age++;
            trail.position = trail.position.add(trail.motion);
            if (trail.gravity != 0.0D) {
                trail.motion = trail.motion.add(0.0D, trail.gravity, 0.0D);
                trail.updateYawPitch();
            }

            if (cameraPos.distanceToSqr(trail.position) > 256.0D * 256.0D) {
                iterator.remove();
                continue;
            }

            if (trail.age >= trail.maxAge) {
                iterator.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        long currentFrame = Minecraft.getInstance().getFrameTimeNs();
        if (currentFrame == lastRenderFrame) {
            return;
        }
        lastRenderFrame = currentFrame;

        if (TRAILS.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Vec3 view = mc.gameRenderer.getMainCamera().position();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.entityCutout(TRAIL_TEXTURE));

        for (TrailState trail : TRAILS.values()) {
            if (!trail.trailVisible) {
                continue;
            }
            renderTrail(trail, poseStack, consumer, view, partialTick);
        }
    }

    private static void renderTrail(TrailState trail, PoseStack poseStack, VertexConsumer consumer, Vec3 view, float partialTick) {
        poseStack.pushPose();

        Vec3 position = trail.position;
        Vec3 motion = trail.motion;
        double bulletX = position.x + motion.x * partialTick;
        double bulletY = position.y + motion.y * partialTick;
        double bulletZ = position.z + motion.z * partialTick;
        poseStack.translate(bulletX - view.x(), bulletY - view.y(), bulletZ - view.z());

        poseStack.mulPose(Axis.YP.rotationDegrees(trail.yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(trail.pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
        poseStack.scale(0.05625F, 0.05625F, 0.05625F);
        poseStack.translate(-4.0F, 0.0F, 0.0F);

        float size = Math.min((trail.age + 1) * 30.0F, 200.0F);
        float tailX = -size;
        float headX = 0.0F;
        float radius = Math.max(1.7F, trail.size * 34.0F);
        int red = (trail.color >> 16) & 0xFF;
        int green = (trail.color >> 8) & 0xFF;
        int blue = trail.color & 0xFF;
        int light = FULL_BRIGHT;

        Matrix4f matrix = poseStack.last().pose();
        vertex(consumer, matrix, red, green, blue, tailX, -radius, -radius, 0.0F, 0.15625F, -1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, tailX, -radius, radius, 0.15625F, 0.15625F, -1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, tailX, radius, radius, 0.15625F, 0.3125F, -1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, tailX, radius, -radius, 0.0F, 0.3125F, -1.0F, 0.0F, 0.0F, light);

        vertex(consumer, matrix, red, green, blue, headX, radius, -radius, 0.0F, 0.15625F, 1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, headX, radius, radius, 0.15625F, 0.15625F, 1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, headX, -radius, radius, 0.15625F, 0.3125F, 1.0F, 0.0F, 0.0F, light);
        vertex(consumer, matrix, red, green, blue, headX, -radius, -radius, 0.0F, 0.3125F, 1.0F, 0.0F, 0.0F, light);

        for (int i = 0; i < 4; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            matrix = poseStack.last().pose();
            vertex(consumer, matrix, red, green, blue, tailX, -radius, radius, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, light);
            vertex(consumer, matrix, red, green, blue, headX, -radius, radius, 0.5F, 0.0F, 0.0F, 1.0F, 0.0F, light);
            vertex(consumer, matrix, red, green, blue, headX, radius, radius, 0.5F, 0.15625F, 0.0F, 1.0F, 0.0F, light);
            vertex(consumer, matrix, red, green, blue, tailX, radius, radius, 0.0F, 0.15625F, 0.0F, 1.0F, 0.0F, light);
        }

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, int red, int green, int blue,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ, int light) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }

    public static void clear() {
        TRAILS.clear();
    }

    private static final class TrailState {
        final int entityId;
        Vec3 position = Vec3.ZERO;
        Vec3 motion = Vec3.ZERO;
        float yaw;
        float pitch;
        int age;
        int maxAge = 3;
        int color = 0xFFFFFF;
        float size = 0.05F;
        double gravity;
        int shooterId = -1;
        boolean trailVisible = true;
        long lastUpdateTick;

        TrailState(int entityId) {
            this.entityId = entityId;
        }

        void updateYawPitch() {
            float horizontalLength = Mth.sqrt((float) (this.motion.x * this.motion.x + this.motion.z * this.motion.z));
            this.yaw = (float) Math.toDegrees(Mth.atan2(this.motion.x, this.motion.z));
            this.pitch = (float) Math.toDegrees(Mth.atan2(this.motion.y, horizontalLength));
        }
    }
}
