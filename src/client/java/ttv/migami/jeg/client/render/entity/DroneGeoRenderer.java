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
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import ttv.migami.jeg.entity.DroneEntity;

/**
 * 3D geo drone with Superb Warfare style yaw/pitch body orientation + spinning rotors.
 */
public final class DroneGeoRenderer extends GeoEntityRenderer<DroneEntity, DroneGeoRenderer.RenderState> {
    private static final String[] ROTOR_BONES = {"wingFL", "wingFR", "wingBL", "wingBR", "propeller", "prop"};
    public static final class RenderState extends EntityRenderState implements GeoRenderState {
        float bodyYaw;
        float bodyPitch;

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

    public DroneGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new DroneGeoModel());
        this.shadowRadius = 0.25F;
    }

    @Override
    public RenderType getRenderType(RenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public RenderState createRenderState(DroneEntity entity, Void context) {
        return new RenderState();
    }

    @Override
    public void captureDefaultRenderState(DroneEntity animatable, Void context, RenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, context, renderState, partialTick);
        ensureAnimatableManager(animatable, context, renderState);
    }

    @Override
    public void extractRenderState(DroneEntity entity, RenderState renderState, float partialTick) {
        ensureAnimatableManager(entity, null, renderState);
        super.extractRenderState(entity, renderState, partialTick);
        renderState.bodyYaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        // SW: model uses body pitch (W/S tilt), not camera/entity xRot
        renderState.bodyPitch = entity.getBodyPitch(partialTick);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<RenderState> passInfo) {
        PoseStack poseStack = passInfo.poseStack();
        RenderState state = passInfo.renderState();
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.bodyPitch));
    }

    /**
     * GeckoLib v5 replacement for v4 {@code preRender} bone spin.
     * Manually rotate SW rotor bones (wingFL/FR/BL/BR) every frame.
     */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<RenderState> passInfo, BoneSnapshots snapshots) {
        // Same continuous spin formula as NeoForge-1.21.1 DroneGeoRenderer.
        float spin = (System.currentTimeMillis() % 36000000L) / 12.0F;
        for (String name : ROTOR_BONES) {
            snapshots.ifPresent(name, snap -> snap.setRotY(spin));
        }
        super.adjustModelBonesForRender(passInfo, snapshots);
    }

    private void ensureAnimatableManager(DroneEntity animatable, Void context, RenderState renderState) {
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
