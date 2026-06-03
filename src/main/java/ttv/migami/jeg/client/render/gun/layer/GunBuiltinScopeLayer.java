package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.CombatScopeGeoModel;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class GunBuiltinScopeLayer extends GeoRenderLayer<AnimatedGunItem> {
    private static final ResourceLocation BOLT_ACTION_RIFLE = Reference.id("bolt_action_rifle");
    private static final double SCOPE_MODEL_Y_OFFSET = -3.0D / 16.0D;
    private final CombatScopeGeoModel scopeModel = new CombatScopeGeoModel();

    public GunBuiltinScopeLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    @Override
    public void renderForBone(
            PoseStack poseStack,
            AnimatedGunItem animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!(getRenderer() instanceof AnimatedGunRenderer renderer)) {
            return;
        }
        ItemStack stack = renderer.getCurrentItemStack();
        if (!"attachment_bone".equals(bone.getName())
                || !BOLT_ACTION_RIFLE.equals(animatable.getStats().id())
                || stack == null
                || !GunScopeSupport.isBoltActionRifleScopeEnabled(stack)) {
            return;
        }

        BakedGeoModel bakedModel = scopeModel.getBakedModel(scopeModel.getModelResource(animatable));
        RenderType scopeRenderType = scopeModel.getRenderType(animatable, scopeModel.getTextureResource(animatable));
        VertexConsumer scopeBuffer = bufferSource.getBuffer(scopeRenderType);
        poseStack.pushPose();
        poseStack.translate(0.0D, SCOPE_MODEL_Y_OFFSET, 0.0D);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, scopeRenderType, scopeBuffer, partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
        poseStack.popPose();
    }
}
