package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

public final class TerrorPhantomRenderer extends MobRenderer<AbstractTerrorPhantom, TerrorPhantomRenderer.RenderState, PhantomModel> {
    public static final class RenderState extends PhantomRenderState {
        Identifier texture = Identifier.withDefaultNamespace("textures/entity/phantom.png");
        float scale = 1.0F;
    }

    public TerrorPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel(context.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(AbstractTerrorPhantom entity, RenderState renderState, float partialTick) {
        super.extractRenderState(entity, renderState, partialTick);
        renderState.flapTime = entity.getUniqueFlapTickOffset() + renderState.ageInTicks;
        renderState.size = entity.getPhantomSize();
        renderState.texture = entity.getRenderTexture();
        renderState.scale = entity.getRenderScale();
    }

    @Override
    public Identifier getTextureLocation(RenderState renderState) {
        return renderState.texture;
    }

    @Override
    protected void scale(RenderState renderState, PoseStack poseStack) {
        float baseScale = 1.0F + 0.15F * renderState.size;
        float scale = baseScale * renderState.scale;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.3125F, 0.1875F);
    }

    @Override
    protected void setupRotations(RenderState renderState, PoseStack poseStack, float ageInTicks, float partialTick) {
        super.setupRotations(renderState, poseStack, ageInTicks, partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(renderState.xRot));
}
}
