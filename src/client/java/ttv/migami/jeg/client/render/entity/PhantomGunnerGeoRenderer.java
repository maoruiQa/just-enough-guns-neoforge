package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerGeoRenderer extends GeoEntityRenderer<PhantomGunner> {
    public PhantomGunnerGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomGunnerGeoModel());
        this.shadowRadius = 0.35F;
    }

    @Override
    public void preRender(
            PoseStack poseStack,
            PhantomGunner animatable,
            BakedGeoModel model,
            @Nullable MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        float baseScale = 1.0F + 0.2F * animatable.getPhantomSize();
        float scale = baseScale * animatable.getGeoScale();
        poseStack.scale(scale, scale, scale);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected void applyRotations(PhantomGunner animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(animatable.getXRot()));
    }
}
