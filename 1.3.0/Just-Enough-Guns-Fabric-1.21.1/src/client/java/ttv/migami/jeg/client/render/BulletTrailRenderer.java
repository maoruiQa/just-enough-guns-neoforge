package ttv.migami.jeg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side bullet trail renderer with two modes:
 * 1. Instant trails (fast bullets): Full raycast on fire, render complete path
 * 2. Dynamic trails (slow bullets): Track entity position each frame
 */
public class BulletTrailRenderer {
    private static final List<TrailSegment> INSTANT_TRAILS = new ArrayList<>();
    private static final int INSTANT_TRAIL_LIFETIME = 3; // Instant trails fade after 3 ticks

    // Track last position for dynamic trails (keyed by entity ID)
    private static final java.util.Map<Integer, Vec3> LAST_POSITIONS = new java.util.HashMap<>();
    private static final java.util.Map<Integer, TrailInfo> DYNAMIC_TRAIL_INFO = new java.util.HashMap<>();

    private static long lastRenderFrame = -1;

    /**
     * Add an instant trail segment (for fast bullets).
     * These are pre-calculated and fade out over time.
     */
    public static void addInstantTrail(Vec3 start, Vec3 end, int color, float size) {
        INSTANT_TRAILS.add(new TrailSegment(start, end, color, size, 0));
    }

    /**
     * Update dynamic trail for a bullet entity (for slow bullets).
     * Tracks entity position and creates trail segments.
     */
    public static void updateDynamicTrail(int entityId, Vec3 currentPos, int color, float size) {
        Vec3 lastPos = LAST_POSITIONS.get(entityId);
        if (lastPos != null && lastPos.distanceToSqr(currentPos) > 0.01) {
            // Create trail segment from last position to current
            INSTANT_TRAILS.add(new TrailSegment(lastPos, currentPos, color, size, 0));
        }
        LAST_POSITIONS.put(entityId, currentPos);
        DYNAMIC_TRAIL_INFO.put(entityId, new TrailInfo(color, size));
    }

    /**
     * Remove dynamic trail tracking for an entity.
     */
    public static void removeDynamicTrail(int entityId) {
        LAST_POSITIONS.remove(entityId);
        DYNAMIC_TRAIL_INFO.remove(entityId);
    }

    public static void tick() {
        // Age and remove old instant trails
        Iterator<TrailSegment> iterator = INSTANT_TRAILS.iterator();
        while (iterator.hasNext()) {
            TrailSegment trail = iterator.next();
            trail.age++;
            if (trail.age > INSTANT_TRAIL_LIFETIME) {
                iterator.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        // Prevent rendering multiple times per frame
        long currentFrame = Minecraft.getInstance().getFrameTimeNs();
        if (currentFrame == lastRenderFrame) {
            return;
        }
        lastRenderFrame = currentFrame;

        if (INSTANT_TRAILS.isEmpty()) {
            return;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        for (TrailSegment trail : INSTANT_TRAILS) {
            float ageRatio = (trail.age + partialTick) / INSTANT_TRAIL_LIFETIME;
            float alpha = Math.max(0.0F, 1.0F - ageRatio);

            renderTrailSegment(consumer, matrix, camera, trail, alpha);
        }
    }

    private static void renderTrailSegment(VertexConsumer consumer, Matrix4f pose, Vec3 camera,
                                          TrailSegment trail, float alpha) {
        Vec3 start = trail.start;
        Vec3 end = trail.end;

        float red = ((trail.color >> 16) & 0xFF) / 255.0F;
        float green = ((trail.color >> 8) & 0xFF) / 255.0F;
        float blue = (trail.color & 0xFF) / 255.0F;

        Vec3 direction = end.subtract(start).normalize();
        Vec3 cameraDir = start.subtract(camera).normalize();
        Vec3 perpendicular = direction.cross(cameraDir).normalize().scale(trail.size * 0.05);

        Vec3 p1 = start.add(perpendicular);
        Vec3 p2 = start.subtract(perpendicular);
        Vec3 p3 = end.subtract(perpendicular);
        Vec3 p4 = end.add(perpendicular);

        float startAlpha = alpha * 0.9F;
        float endAlpha = alpha * 0.3F;

        drawVertex(consumer, pose, camera, p1, red, green, blue, startAlpha);
        drawVertex(consumer, pose, camera, p2, red, green, blue, startAlpha);
        drawVertex(consumer, pose, camera, p3, red, green, blue, endAlpha);
        drawVertex(consumer, pose, camera, p4, red, green, blue, endAlpha);
    }

    private static void drawVertex(VertexConsumer consumer, Matrix4f pose, Vec3 camera, Vec3 pos,
                                   float r, float g, float b, float a) {
        consumer.addVertex(pose,
                (float)(pos.x - camera.x()),
                (float)(pos.y - camera.y()),
                (float)(pos.z - camera.z()))
                .setColor(r, g, b, a)
                .setUv(0, 0)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0, 1, 0);
    }

    public static void clear() {
        INSTANT_TRAILS.clear();
        LAST_POSITIONS.clear();
        DYNAMIC_TRAIL_INFO.clear();
    }

    private static class TrailSegment {
        final Vec3 start;
        final Vec3 end;
        final int color;
        final float size;
        int age;

        TrailSegment(Vec3 start, Vec3 end, int color, float size, int age) {
            this.start = start;
            this.end = end;
            this.color = color;
            this.size = size;
            this.age = age;
        }
    }

    private static class TrailInfo {
        final int color;
        final float size;

        TrailInfo(int color, float size) {
            this.color = color;
            this.size = size;
        }
    }
}
