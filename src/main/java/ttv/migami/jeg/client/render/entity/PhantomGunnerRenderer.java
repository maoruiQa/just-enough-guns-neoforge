package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerRenderer extends MobRenderer<PhantomGunner, PhantomRenderState, PhantomModel> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/entity/phantom_gunner/phantom_gunner.png");

    public PhantomGunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.85F);
        this.addLayer(new PhantomEyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomRenderState state) {
        return TEXTURE;
    }

    @Override
    public PhantomRenderState createRenderState() {
        return new PhantomRenderState();
    }

    @Override
    public void extractRenderState(PhantomGunner entity, PhantomRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.flapTime = entity.getUniqueFlapTickOffset() + state.ageInTicks;
        state.size = entity.getPhantomSize();
    }

    @Override
    protected void scale(PhantomRenderState state, PoseStack poseStack) {
        float scale = 1.0F + 0.2F * state.size;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.3125F, 0.1875F);
    }

    @Override
    protected void setupRotations(PhantomRenderState state, PoseStack poseStack, float yaw, float partialTick) {
        super.setupRotations(state, poseStack, yaw, partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
    }
}
