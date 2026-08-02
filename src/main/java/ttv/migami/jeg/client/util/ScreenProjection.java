package ttv.migami.jeg.client.util;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ttv.migami.jeg.Reference;

/**
 * Lightweight world->screen projection (Superb Warfare VectorUtil style).
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ScreenProjection {
    private static Matrix4f modelView = new Matrix4f();
    private static Matrix4f projection = new Matrix4f();
    private static double fov = 70.0D;

    private ScreenProjection() {}

    public static void captureMatrices(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        modelView = new Matrix4f(modelViewMatrix);
        projection = new Matrix4f(projectionMatrix);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (true) { // FOV event always updates cache
            fov = event.getFOV();
        }
    }

    @Nullable
    public static Vec3 worldToScreen(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) {
            return null;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        Vector4f rel = new Vector4f(
                (float) (worldPos.x - camera.position().x),
                (float) (worldPos.y - camera.position().y),
                (float) (worldPos.z - camera.position().z),
                1.0F
        );
        rel.mul(modelView);
        rel.mul(projection);
        float depth = rel.w;
        if (depth == 0.0F) {
            return null;
        }
        rel.div(depth);
        if (depth < 0.05F) {
            return null;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        return new Vec3(w * (0.5D + rel.x * 0.5D), h * (0.5D - rel.y * 0.5D), depth);
    }

    /** Fallback when matrices are not yet captured. */
    @Nullable
    public static Vec3 approximateWorldToScreen(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        Vec3 look = new Vec3(camera.forwardVector());
        Vec3 right = new Vec3(camera.leftVector()).scale(-1.0D);
        Vec3 up = new Vec3(camera.upVector());
        Vec3 rel = worldPos.subtract(cam);
        double depth = rel.dot(look);
        if (depth <= 0.15D) {
            return null;
        }
        double aspect = (double) mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getGuiScaledHeight());
        double fovRad = Math.toRadians(fov);
        double scale = 1.0D / Math.tan(fovRad * 0.5D);
        double x = rel.dot(right) * scale / (depth * aspect);
        double y = rel.dot(up) * scale / depth;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        return new Vec3(w * (0.5D + x * 0.5D), h * (0.5D - y * 0.5D), depth);
    }

    public static boolean canSee(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.player == null) {
            return false;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 to = worldPos.subtract(camera.position()).normalize();
        Vec3 look = new Vec3(camera.forwardVector());
        return look.dot(to) > 0.0D;
    }
}
