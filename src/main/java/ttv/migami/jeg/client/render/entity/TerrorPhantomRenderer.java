package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.entity.monster.phantom.AbstractTerrorPhantom;

public final class TerrorPhantomRenderer extends MobRenderer<AbstractTerrorPhantom, PhantomModel<AbstractTerrorPhantom>> {
    private static final ResourceLocation FALLBACK = ResourceLocation.withDefaultNamespace("textures/entity/phantom.png");

    public TerrorPhantomRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
        this.addLayer(new PhantomEyesLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractTerrorPhantom entity) {
        ResourceLocation tex = entity.getRenderTexture();
        return tex != null ? tex : FALLBACK;
    }

    @Override
    protected void scale(AbstractTerrorPhantom entity, PoseStack poseStack, float partialTickTime) {
        float baseScale = 1.0F + 0.15F * entity.getPhantomSize();
        float scale = baseScale * entity.getRenderScale();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.3125F, 0.1875F);
    }

}
