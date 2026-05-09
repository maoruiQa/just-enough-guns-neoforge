package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

final class VehicleGlowLayer extends GeoRenderLayer<VehicleEntity> {
    VehicleGlowLayer(GeoRenderer<VehicleEntity> renderer) {
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
        if (!DefaultVehicleResource.hasGlowTexture(animatable)) {
            return;
        }
        RenderType glowRenderType = RenderType.entityTranslucentEmissive(DefaultVehicleResource.glowTexture(animatable));
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);
        getRenderer().reRender(
                bakedModel,
                poseStack,
                bufferSource,
                animatable,
                glowRenderType,
                glowBuffer,
                partialTick,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }
}
