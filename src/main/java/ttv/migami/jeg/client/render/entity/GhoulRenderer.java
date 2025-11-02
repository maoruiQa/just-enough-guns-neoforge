package ttv.migami.jeg.client.render.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import ttv.migami.jeg.Reference;

public final class GhoulRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE = Reference.id("textures/entity/zombie/ghoul.png");

    public GhoulRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState state) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(Zombie entity, BlockPos pos) {
        return 7;
    }
}
