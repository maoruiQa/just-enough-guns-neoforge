package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import ttv.migami.jeg.entity.BulletEntity;

public final class BulletRenderer extends EntityRenderer<BulletEntity, BulletRenderer.State> {
    private static final ResourceLocation FLAMETHROWER_ID = ResourceLocation.parse("jeg:flamethrower");
    private static final ResourceLocation FLARE_GUN_ID = ResourceLocation.parse("jeg:flare_gun");
    private static final ResourceLocation ROCKET_LAUNCHER_ID = ResourceLocation.parse("jeg:rocket_launcher");
    private static final ResourceLocation HYPERSONIC_ID = ResourceLocation.parse("jeg:hypersonic_cannon");
    private static final ResourceLocation GRENADE_LAUNCHER_ID = ResourceLocation.parse("jeg:grenade_launcher");
    private static final ResourceLocation TYPHOONEE_ID = ResourceLocation.parse("jeg:typhoonee");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void submit(State state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // For SLOW bullets (projectiles), update dynamic trail
        if (state.isSlowBullet && state.entityId >= 0) {
            ttv.migami.jeg.client.render.BulletTrailRenderer.updateDynamicTrail(
                state.entityId,
                new Vec3(state.x, state.y, state.z),
                state.trailColor,
                state.size
            );
        }

        // Call global trail renderer once per frame
        com.mojang.blaze3d.vertex.PoseStack globalPoseStack = new com.mojang.blaze3d.vertex.PoseStack();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        ttv.migami.jeg.client.render.BulletTrailRenderer.render(
            globalPoseStack,
            bufferSource,
            partialTick
        );
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BulletEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entityId = entity.getId();
        state.size = entity.getProjectileSize();
        state.trailColor = entity.getTrailColor();

        // Determine if this is a slow bullet that needs dynamic trail tracking
        ResourceLocation gunId = ResourceLocation.parse(entity.getGunId());
        state.isSlowBullet = isSlowBullet(gunId);
    }

    private boolean isSlowBullet(ResourceLocation gunId) {
        return gunId.equals(FLAMETHROWER_ID) ||
               gunId.equals(FLARE_GUN_ID) ||
               gunId.equals(ROCKET_LAUNCHER_ID) ||
               gunId.equals(HYPERSONIC_ID) ||
               gunId.equals(GRENADE_LAUNCHER_ID) ||
               gunId.equals(TYPHOONEE_ID);
    }

    public static final class State extends EntityRenderState {
        int entityId = -1;
        float size;
        int trailColor;
        boolean isSlowBullet;
    }
}
