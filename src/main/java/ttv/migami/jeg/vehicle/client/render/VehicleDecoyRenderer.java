package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.vehicle.projectile.VehicleDecoyEntity;

public final class VehicleDecoyRenderer extends EntityRenderer<VehicleDecoyEntity, VehicleDecoyRenderer.State> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/redstone_torch.png");

    public VehicleDecoyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (state.smokeDecoy) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.scale(0.35F, 0.35F, 0.35F);
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            // Use the deferred pose snapshot, not poseStack after popPose().
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.entityTranslucent(TEXTURE),
                    (pose, buffer) -> renderUnitCube(pose, buffer, state.lightCoords)
            );
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VehicleDecoyEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.smokeDecoy = entity.isSmokeDecoy();
    }

    public Identifier getTextureLocation(VehicleDecoyEntity entity) {
        return TEXTURE;
    }

    public static final class State extends EntityRenderState {
        boolean smokeDecoy;
    }

    private static void renderUnitCube(PoseStack.Pose pose, VertexConsumer buffer, int packedLight) {
        face(pose, buffer, packedLight, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1);
        face(pose, buffer, packedLight, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, -1);
        face(pose, buffer, packedLight, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, -1, 0, 0);
        face(pose, buffer, packedLight, 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0);
        face(pose, buffer, packedLight, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0);
        face(pose, buffer, packedLight, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, -1, 0);
    }

    private static void face(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int packedLight,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz
    ) {
        vertex(pose, buffer, packedLight, x1, y1, z1, 0, 1, nx, ny, nz);
        vertex(pose, buffer, packedLight, x2, y2, z2, 1, 1, nx, ny, nz);
        vertex(pose, buffer, packedLight, x3, y3, z3, 1, 0, nx, ny, nz);
        vertex(pose, buffer, packedLight, x4, y4, z4, 0, 0, nx, ny, nz);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            int packedLight,
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz
    ) {
        buffer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}
