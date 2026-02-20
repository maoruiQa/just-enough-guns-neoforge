package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerRenderer extends MobRenderer<PhantomGunner, PhantomModel<PhantomGunner>> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/entity/phantom_gunner/phantom_gunner.png");

    public PhantomGunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomModel<>(context.bakeLayer(ModelLayers.PHANTOM)), 0.85F);
        this.addLayer(new PhantomEyesLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomGunner entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(PhantomGunner entity, PoseStack poseStack, float partialTickTime) {
        float scale = 1.0F + 0.2F * entity.getPhantomSize();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0F, 1.3125F, 0.1875F);
    }

}
