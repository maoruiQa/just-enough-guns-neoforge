package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.entity.BulletEntity;

public final class BulletRenderer extends EntityRenderer<BulletEntity, BulletRenderer.State> {
    private static final Identifier FLAMETHROWER_ID = Identifier.parse("jeg:flamethrower");
    private static final Identifier FLARE_GUN_ID = Identifier.parse("jeg:flare_gun");
    private static final Identifier ROCKET_LAUNCHER_ID = Identifier.parse("jeg:rocket_launcher");
    private static final Identifier HYPERSONIC_ID = Identifier.parse("jeg:hypersonic_cannon");
    private static final Identifier GRENADE_LAUNCHER_ID = Identifier.parse("jeg:grenade_launcher");
    private static final Identifier TYPHOONEE_ID = Identifier.parse("jeg:typhoonee");

    public BulletRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public void submit(State state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Trails are rendered globally from GunClientEvents.onRenderLevelAfterEntities.
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
        Identifier gunId = Identifier.parse(entity.getGunId());
        state.isSlowBullet = isSlowBullet(gunId);
    }

    private boolean isSlowBullet(Identifier gunId) {
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
