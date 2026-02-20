package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

/**
 * Renderer for AbstractTerrorPhantom using the legacy rendering system (1.21.1).
 * EntityRenderState was introduced in 1.21.2+, so this uses direct entity rendering.
 */
public final class TerrorPhantomRenderer extends MobRenderer<AbstractTerrorPhantom, PhantomModel<AbstractTerrorPhantom>> {

    public TerrorPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractTerrorPhantom entity) {
        return entity.getRenderTexture();
    }

    @Override
    protected void scale(AbstractTerrorPhantom entity, PoseStack poseStack, float partialTickTime) {
        int size = entity.getPhantomSize();
        float baseScale = 1.0F + 0.15F * size;
        float scale = baseScale * entity.getRenderScale();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.3125F, 0.1875F);
    }

    @Override
    protected void setupRotations(AbstractTerrorPhantom entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
    }
}
