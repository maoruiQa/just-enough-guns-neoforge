package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;

public final class SpecialExplosiveGeoRenderer extends GeoEntityRenderer<PlacedExplosiveEntity, SpecialExplosiveGeoRenderer.RenderState> {
    public static final class RenderState extends EntityRenderState implements GeoRenderState {
        float bodyYaw;
        float bodyPitch;
        boolean settled;

        @Override
        @SuppressWarnings("unchecked")
        public Map<DataTicket<?>, Object> getDataMap() {
            try {
                return (Map<DataTicket<?>, Object>) GeckoLibStateFieldHolder.FIELD.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access GeckoLib render-state data map", e);
            }
        }

        private static final class GeckoLibStateFieldHolder {
            private static final Field FIELD = findField();

            private static Field findField() {
                try {
                    Field f = EntityRenderState.class.getDeclaredField("geckolib$data");
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("GeckoLib render-state field 'geckolib$data' not found. Is GeckoLib loaded?", e);
                }
            }
        }
    }

    public SpecialExplosiveGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new SpecialExplosiveGeoModel());
        this.shadowRadius = 0.2F;
    }

    @Override
    public RenderType getRenderType(RenderState renderState, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    @Override
    public RenderState createRenderState(PlacedExplosiveEntity entity, Void context) {
        return new RenderState();
    }

    @Override
    public void captureDefaultRenderState(PlacedExplosiveEntity animatable, Void context, RenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);
        ensureAnimatableManager(animatable, context, renderState);
    }

    @Override
    public void extractRenderState(PlacedExplosiveEntity entity, RenderState renderState, float partialTick) {
        ensureAnimatableManager(entity, null, renderState);
        super.extractRenderState(entity, renderState, partialTick);
        renderState.bodyYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        renderState.bodyPitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<RenderState> passInfo) {
        PoseStack poseStack = passInfo.poseStack();
        RenderState state = passInfo.renderState();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.bodyPitch));
    }

    private void ensureAnimatableManager(PlacedExplosiveEntity animatable, Void context, RenderState renderState) {
        AnimatableManager<?> manager = renderState.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_MANAGER, (AnimatableManager<?>) null);
        if (manager != null) {
            return;
        }
        long id = getInstanceId(animatable, context);
        AnimatableManager<?> cacheManager = null;
        var cache = animatable.getAnimatableInstanceCache();
        if (cache != null) {
            cacheManager = cache.getManagerForId(id);
            if (cacheManager == null) {
                cacheManager = cache.getManagerForId(0L);
            }
        }
        if (cacheManager == null) {
            cacheManager = new AnimatableManager<>(animatable);
        }
        renderState.addGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, id);
        renderState.addGeckolibData(DataTickets.ANIMATABLE_MANAGER, cacheManager);
    }
}
