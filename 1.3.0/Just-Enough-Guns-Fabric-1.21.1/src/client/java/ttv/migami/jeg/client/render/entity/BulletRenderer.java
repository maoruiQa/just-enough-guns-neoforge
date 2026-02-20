package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.BulletEntity;

/**
 * Renderer for BulletEntity using the legacy rendering system (1.21.1).
 * EntityRenderState was introduced in 1.21.2+, so this uses direct entity rendering.
 */
public final class BulletRenderer extends EntityRenderer<BulletEntity> {
    private static final ResourceLocation FLAMETHROWER_ID = ResourceLocation.parse("jeg:flamethrower");
    private static final ResourceLocation FLARE_GUN_ID = ResourceLocation.parse("jeg:flare_gun");
    private static final ResourceLocation ROCKET_LAUNCHER_ID = ResourceLocation.parse("jeg:rocket_launcher");
    private static final ResourceLocation HYPERSONIC_ID = ResourceLocation.parse("jeg:hypersonic_cannon");
    private static final ResourceLocation GRENADE_LAUNCHER_ID = ResourceLocation.parse("jeg:grenade_launcher");
    private static final ResourceLocation TYPHOONEE_ID = ResourceLocation.parse("jeg:typhoonee");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(BulletEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // For SLOW bullets (projectiles), update dynamic trail
        ResourceLocation gunId = ResourceLocation.parse(entity.getGunId());
        boolean isSlowBullet = isSlowBullet(gunId);

        if (isSlowBullet) {
            ttv.migami.jeg.client.render.BulletTrailRenderer.updateDynamicTrail(
                entity.getId(),
                entity.position(),
                entity.getTrailColor(),
                entity.getProjectileSize()
            );
        }

        // Call global trail renderer once per frame
        PoseStack globalPoseStack = new PoseStack();
        Minecraft mc = Minecraft.getInstance();
        float gameTimeDelta = mc.getTimer().getGameTimeDeltaPartialTick(false);

        ttv.migami.jeg.client.render.BulletTrailRenderer.render(
            globalPoseStack,
            bufferSource,
            gameTimeDelta
        );
    }

    @Override
    public ResourceLocation getTextureLocation(BulletEntity entity) {
        return null; // Bullets don't use textures, they use particle trails
    }

    private boolean isSlowBullet(ResourceLocation gunId) {
        return gunId.equals(FLAMETHROWER_ID) ||
               gunId.equals(FLARE_GUN_ID) ||
               gunId.equals(ROCKET_LAUNCHER_ID) ||
               gunId.equals(HYPERSONIC_ID) ||
               gunId.equals(GRENADE_LAUNCHER_ID) ||
               gunId.equals(TYPHOONEE_ID);
    }
}
