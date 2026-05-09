package ttv.migami.jeg.vehicle.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoRenderer extends GeoEntityRenderer<VehicleEntity> {
    public VehicleGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new VehicleGeoModel());
        this.addRenderLayer(new VehicleGlowLayer(this));
        this.shadowRadius = 0.9F;
    }

    @Override
    protected void applyRotations(VehicleEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
    }
}
