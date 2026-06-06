package ttv.migami.jeg.client.render.gun.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.render.gun.AnimatedGunRenderer;
import ttv.migami.jeg.client.render.gun.CombatScopeGeoModel;
import ttv.migami.jeg.gun.GunScopeSupport;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class GunBuiltinScopeLayer extends GeoRenderLayer<AnimatedGunItem, GeoItemRenderer.RenderData, GeoRenderState> {
    private static final Identifier BOLT_ACTION_RIFLE = Reference.id("bolt_action_rifle");
    private static final Identifier SCOPE_TEXTURE = Reference.id("textures/animated/attachment/combat_scope.png");
    private static final double SCOPE_MODEL_Y_OFFSET = -4.0D / 16.0D;
    private final CombatScopeGeoModel scopeModel = new CombatScopeGeoModel();

    public GunBuiltinScopeLayer(GeoItemRenderer<AnimatedGunItem> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<GeoRenderState> passInfo, SubmitNodeCollector collector) {
        Item item = passInfo.renderState().getOrDefaultGeckolibData(AnimatedGunRenderer.ANIMATED_ITEM, (Item) null);
        if (!(item instanceof AnimatedGunItem gun)
                || !BOLT_ACTION_RIFLE.equals(gun.getStats().id())
                || !GunScopeSupport.isBoltActionRifleScopeEnabled()) {
            return;
        }

        passInfo.model().getBone("attachment_bone")
                .ifPresent(bone -> passInfo.addPerBoneRender(bone, new ScopeRenderTask(scopeModel)));
    }

    private static final class ScopeRenderTask implements PerBoneRender<GeoRenderState> {
        private final CombatScopeGeoModel scopeModel;

        private ScopeRenderTask(CombatScopeGeoModel scopeModel) {
            this.scopeModel = scopeModel;
        }

        @Override
        public void submitRenderTask(RenderPassInfo<GeoRenderState> passInfo, GeoBone bone, SubmitNodeCollector collector) {
            BakedGeoModel bakedModel = scopeModel.getBakedModel(Reference.id("item/attachment/combat_scope"));
            collector.submitCustomGeometry(
                    passInfo.poseStack(),
                    RenderTypes.entityTranslucent(SCOPE_TEXTURE),
                    (pose, buffer) -> {
                        PoseStack scopePose = passInfo.poseStack();
                        scopePose.pushPose();
                        try {
                            scopePose.last().set(pose);
                            scopePose.translate(0.0D, SCOPE_MODEL_Y_OFFSET, 0.0D);
                            passInfo.renderPosed(() -> bakedModel.render(passInfo, buffer, passInfo.packedLight(), passInfo.packedOverlay(), passInfo.renderColor()));
                        } finally {
                            scopePose.popPose();
                        }
                    }
            );
        }
    }
}
