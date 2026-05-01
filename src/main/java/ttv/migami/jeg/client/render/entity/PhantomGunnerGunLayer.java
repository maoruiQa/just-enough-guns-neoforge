package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;
import ttv.migami.jeg.item.GunItem;

final class PhantomGunnerGunLayer extends GeoRenderLayer<PhantomGunner> {
    private static final String ANCHOR_BONE = "body";

    PhantomGunnerGunLayer(GeoRenderer<PhantomGunner> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(
            PoseStack poseStack,
            PhantomGunner animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!ANCHOR_BONE.equals(bone.getName())) {
            return;
        }

        ItemStack stack = animatable.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof GunItem)) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(-0.1D, -0.45D, -1.2D);
        poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.scale(0.65F, 0.65F, 0.65F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                animatable,
                stack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                bufferSource,
                animatable.level(),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                animatable.getId()
        );
        poseStack.popPose();
    }

    @Override
    public void render(
            PoseStack poseStack,
            PhantomGunner animatable,
            BakedGeoModel bakedModel,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        // Per-bone only.
    }
}
