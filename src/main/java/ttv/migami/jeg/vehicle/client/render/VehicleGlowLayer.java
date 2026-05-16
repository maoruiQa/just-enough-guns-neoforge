package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Brightness;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

final class VehicleGlowLayer<T extends VehicleEntity> extends GeoRenderLayer<T, Void, VehicleRenderState> {
    VehicleGlowLayer(GeoRenderer<T, Void, VehicleRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<VehicleRenderState> passInfo, SubmitNodeCollector collector) {
        VehicleEntity animatable = passInfo.renderState().vehicle;
        if (!DefaultVehicleResource.hasGlowTexture(animatable)) {
            return;
        }

        PoseStack poseStack = passInfo.poseStack();
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucentEmissive(DefaultVehicleResource.glowTexture(animatable)),
                (pose, buffer) -> {
                    PoseStack glowPose = new PoseStack();
                    glowPose.last().set(pose);
                    passInfo.renderPosed(() -> {
                        for (GeoBone bone : passInfo.model().topLevelBones()) {
                            bone.render(passInfo, glowPose, buffer, Brightness.FULL_BRIGHT.pack(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                        }
                    });
                }
        );
    }
}
