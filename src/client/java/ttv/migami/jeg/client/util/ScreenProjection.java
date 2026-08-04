package ttv.migami.jeg.client.util;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Lightweight world-to-screen projection (Superb Warfare VectorUtil style).
 * <p>
 * On 1.21.x, {@code GameRenderer.renderLevel} keeps camera look in a separate model-view
 * ({@code rotation(camera.rotation().conjugate())}), not on the entity PoseStack.
 * Capturing PoseStack alone is identity-like and mis-projects northward (−Z) targets.
 */
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

    public static void setFov(double value) {
        fov = value;
    }

    @Nullable
    public static Vec3 worldToScreen(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) {
            return null;
        }
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 relWorld = worldPos.subtract(camera.getPosition());
        double lookDepth = relWorld.dot(new Vec3(camera.getLookVector()));
        if (lookDepth < 0.05D) {
            return null;
        }

        // Primary: rebuild view from camera rotation (matches GameRenderer.renderLevel model-view).
        Matrix4f view = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        Vector4f rel = new Vector4f(
                (float) relWorld.x,
                (float) relWorld.y,
                (float) relWorld.z,
                1.0F
        );
        rel.mul(view);

        if (matricesValid) {
            rel.mul(projection);
        } else {
            // Secondary: live projection from FOV when world capture has not run yet.
            try {
                rel.mul(mc.gameRenderer.getProjectionMatrix(fov));
            } catch (Throwable ignored) {
                return null;
            }
        }

        float depth = rel.w;
        if (depth == 0.0F || depth < 0.05F) {
            return null;
        }
        rel.div(depth);
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        return new Vec3(w * (0.5D + rel.x * 0.5D), h * (0.5D - rel.y * 0.5D), depth);
    }

    @Nullable
    public static Vec3 approximateWorldToScreen(Vec3 worldPos) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector());
        Vec3 right = new Vec3(camera.getLeftVector()).scale(-1.0D);
        Vec3 up = new Vec3(camera.getUpVector());
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
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 to = worldPos.subtract(camera.getPosition()).normalize();
        Vec3 look = new Vec3(camera.getLookVector());
        // Match NeoForge-1.21.1 / SW-style FOV cone (not just hemisphere).
        double dot = net.minecraft.util.Mth.clamp(to.dot(look), -1.0D, 1.0D);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle < fov + 12.0D;
    }
}
