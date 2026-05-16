package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

final class VehicleBoatWaterMaskLayer extends GeoRenderLayer<VehicleEntity> {
    private static final ResourceLocation WATER_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/water_still.png");
    private static final float HALF_WIDTH = 1.05F;
    private static final float HALF_LENGTH = 1.72F;
    private static final float WATERLINE_Y = 0.24F;
    private static final int WATER_RED = 81;
    private static final int WATER_GREEN = 140;
    private static final int WATER_BLUE = 216;
    private static final int WATER_ALPHA = 136;

    VehicleBoatWaterMaskLayer(GeoRenderer<VehicleEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            VehicleEntity animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (animatable.vehicleData().defaults().vehicleType() != VehicleType.BOAT || !animatable.isInWater()) {
            return;
        }

        VertexConsumer waterBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(WATER_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        quad(waterBuffer, matrix, packedLight, 0.0F, 1.0F, 0.0F,
                -HALF_WIDTH, WATERLINE_Y, -HALF_LENGTH,
                HALF_WIDTH, WATERLINE_Y, -HALF_LENGTH,
                HALF_WIDTH, WATERLINE_Y, HALF_LENGTH,
                -HALF_WIDTH, WATERLINE_Y, HALF_LENGTH);
        quad(waterBuffer, matrix, packedLight, 0.0F, -1.0F, 0.0F,
                -HALF_WIDTH, WATERLINE_Y, HALF_LENGTH,
                HALF_WIDTH, WATERLINE_Y, HALF_LENGTH,
                HALF_WIDTH, WATERLINE_Y, -HALF_LENGTH,
                -HALF_WIDTH, WATERLINE_Y, -HALF_LENGTH);
    }

    private static void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4
    ) {
        vertex(consumer, matrix, packedLight, normalX, normalY, normalZ, x1, y1, z1, 0.0F, 0.0F);
        vertex(consumer, matrix, packedLight, normalX, normalY, normalZ, x2, y2, z2, 1.0F, 0.0F);
        vertex(consumer, matrix, packedLight, normalX, normalY, normalZ, x3, y3, z3, 1.0F, 1.0F);
        vertex(consumer, matrix, packedLight, normalX, normalY, normalZ, x4, y4, z4, 0.0F, 1.0F);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ,
            float x,
            float y,
            float z,
            float u,
            float v
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(WATER_RED, WATER_GREEN, WATER_BLUE, WATER_ALPHA)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(normalX, normalY, normalZ);
    }
}
