package ttv.migami.jeg.vehicle.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.client.render.block.VehicleBlockRenderState;

public final class VehicleAssemblingTableBlockLayer extends GeoRenderLayer<VehicleAssemblingTableBlockEntity, Void, VehicleBlockRenderState> {
    private static final Identifier LAYER = Reference.id("textures/block/vehicle_assembling_table_e.png");

    public VehicleAssemblingTableBlockLayer(GeoRenderer<VehicleAssemblingTableBlockEntity, Void, VehicleBlockRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<VehicleBlockRenderState> passInfo, SubmitNodeCollector collector) {
        if (passInfo.renderState().night) {
            PoseStack poseStack = passInfo.poseStack();
            collector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.eyes(LAYER),
                    (pose, buffer) -> {
                        PoseStack glowPose = new PoseStack();
                        glowPose.last().set(pose);
                        passInfo.renderPosed(() -> {
                            for (GeoBone bone : passInfo.model().topLevelBones()) {
                                bone.render(passInfo, glowPose, buffer, passInfo.packedLight(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
                            }
                        });
                    }
            );
        }
    }
}
