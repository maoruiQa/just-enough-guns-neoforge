package ttv.migami.jeg.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.constant.dataticket.DataTicket;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;
import software.bernie.geckolib.renderer.base.RenderPassInfo;
import ttv.migami.jeg.entity.monster.phantom.PhantomGunner;

public final class PhantomGunnerGeoRenderer extends GeoEntityRenderer<PhantomGunner, PhantomGunnerGeoRenderer.RenderState> {
    public static final class RenderState extends PhantomRenderState implements GeoRenderState {
        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            // Must return GeckoLib's injected map (EntityRenderState.geckolib$data), otherwise GeckoLib
            // reads/writes different maps and can crash (missing ANIMATABLE_MANAGER).
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

    public PhantomGunnerGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomGunnerGeoModel());
        this.shadowRadius = 0.35F;
    }

    @Override
    public RenderState createRenderState(PhantomGunner entity, Void context) {
        return new RenderState();
    }

    @Override
    public void captureDefaultRenderState(PhantomGunner animatable, Void context, RenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);

        AnimatableManager<?> manager = renderState.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_MANAGER, (AnimatableManager<?>) null);
        if (manager == null) {
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

    @Override
    public void extractRenderState(PhantomGunner entity, RenderState renderState, float partialTick) {
        AnimatableManager<?> manager = renderState.getOrDefaultGeckolibData(DataTickets.ANIMATABLE_MANAGER, (AnimatableManager<?>) null);
        if (manager == null) {
            long id = getInstanceId(entity, null);
            AnimatableManager<?> cacheManager = null;
            var cache = entity.getAnimatableInstanceCache();
            if (cache != null) {
                cacheManager = cache.getManagerForId(id);
                if (cacheManager == null) {
                    cacheManager = cache.getManagerForId(0L);
                }
            }
            if (cacheManager == null) {
                cacheManager = new AnimatableManager<>(entity);
            }

            renderState.addGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID, id);
            renderState.addGeckolibData(DataTickets.ANIMATABLE_MANAGER, cacheManager);
        }

        super.extractRenderState(entity, renderState, partialTick);

        renderState.flapTime = entity.getUniqueFlapTickOffset() + renderState.ageInTicks;
        renderState.size = entity.getPhantomSize();
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<RenderState> passInfo, float width, float height) {
        PhantomGunner animatable = passInfo.getOrDefaultGeckolibData(PhantomGunnerGeoModel.ANIMATABLE, null);
        if (animatable != null) {
            float baseScale = 1.0F + 0.2F * passInfo.renderState().size;
            float scale = baseScale * animatable.getGeoScale();
            PoseStack poseStack = passInfo.poseStack();
            poseStack.scale(scale, scale, scale);
            return;
        }

        super.scaleModelForRender(passInfo, width, height);
    }
}

