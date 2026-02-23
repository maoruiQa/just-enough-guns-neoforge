package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.faction.raid.RaidEntity;

/**
 * Legacy renderer for MC 1.21.1 where EntityRenderState does not exist yet.
 * RaidEntity is a logical controller entity and intentionally renders nothing.
 */
public final class RaidEntityRenderer extends EntityRenderer<RaidEntity> {
    private static final ResourceLocation DUMMY_TEXTURE = TextureAtlas.LOCATION_BLOCKS;

    public RaidEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RaidEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Intentionally empty: this entity has no visible model.
    }

    @Override
    public ResourceLocation getTextureLocation(RaidEntity entity) {
        return DUMMY_TEXTURE;
    }
}
