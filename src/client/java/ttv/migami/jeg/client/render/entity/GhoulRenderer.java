package ttv.migami.jeg.client.render.entity;

import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.monster.Ghoul;

public final class GhoulRenderer extends MobRenderer<Ghoul, ZombieModel<Ghoul>> {
    private static final ResourceLocation TEXTURE = Reference.id("textures/entity/zombie/ghoul.png");

    public GhoulRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(Ghoul entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(Ghoul entity, net.minecraft.core.BlockPos pos) {
        return 7;
    }
}
