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
 * Lightweight world-to-screen projection (Superb Warfare VectorUtil style).
 * <p>
 * On 26.x, prefer {@link net.minecraft.client.renderer.GameRenderer#projectPointToScreen(Vec3)}
 * which multiplies camera view-rotation × projection. Manual MVP from incomplete model-view
 * (e.g. empty poseStack at bobHurt) incorrectly projects the north (−Z) axis only.
 */
@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ScreenProjection {
    private static Matrix4f modelView = new Matrix4f();
    private static Matrix4f projection = new Matrix4f();
    private static double fov = 70.0D;
    private static boolean matricesValid;

    private ScreenProjection() {}

    public static void captureMatrices(Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        modelView = new Matrix4f(modelViewMatrix);
        projection = new Matrix4f(projectionMatrix);
        matricesValid = true;
    }

    public static double getFov() {
        return fov;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFov(ViewportEvent.ComputeFov event) {
        fov = event.getFOV();
    }

    @Nullable
    public static Vec3 worldToScreen(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) {
            return null;
        }
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 relWorld = worldPos.subtract(camera.position());
        double lookDepth = relWorld.dot(new Vec3(camera.forwardVector()));
        if (lookDepth < 0.05D) {
            return null;
        }

        // Primary: vanilla 26.x projector (viewRotation × projection → NDC).
        try {
            Vec3 ndc = mc.gameRenderer.projectPointToScreen(worldPos);
            if (ndc != null && !Double.isNaN(ndc.x) && !Double.isNaN(ndc.y)) {
                int w = mc.getWindow().getGuiScaledWidth();
                int h = mc.getWindow().getGuiScaledHeight();
                return new Vec3(w * (0.5D + ndc.x * 0.5D), h * (0.5D - ndc.y * 0.5D), lookDepth);
            }
        } catch (Throwable ignored) {
            // Fall through to captured matrices / approximate path.
        }

        // Secondary: captured modelView + projection.
        if (!matricesValid) {
            // Allow first frames before AfterLevel capture when matrices were never set.
            // Keep previous NeoForge behavior of always trying mul when capture ran at least once;
            // if never captured, matrices are identity — skip to avoid north-only false positives.
            return null;
        }
        Vector4f rel = new Vector4f(
                (float) relWorld.x,
                (float) relWorld.y,
                (float) relWorld.z,
                1.0F
        );
        rel.mul(modelView);
        rel.mul(projection);
        float depth = rel.w;
        if (depth == 0.0F || depth < 0.05F) {
            return null;
        }
        rel.div(depth);
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
        // Match NeoForge-1.21.1 / SW-style FOV cone (not just hemisphere).
        double dot = Mth.clamp(to.dot(look), -1.0D, 1.0D);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle < fov + 12.0D;
    }
}
