package ttv.migami.jeg.client.render.entity;

import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractIllager;
import ttv.migami.jeg.entity.GunnerEntity;

public class GunnerRenderer extends MobRenderer<GunnerEntity, IllagerRenderState, IllagerModel<IllagerRenderState>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/illager/pillager.png");

    public GunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public IllagerRenderState createRenderState() {
        return new IllagerRenderState();
    }

    @Override
    public void extractRenderState(GunnerEntity entity, IllagerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, this.itemModelResolver);
        state.isAggressive = entity.isAggressive();
        state.mainArm = entity.getMainArm();
        state.attackAnim = entity.getAttackAnim(partialTicks);
        state.isRiding = entity.isPassenger();
        // Always use CROSSBOW_HOLD for gun wielding to prevent texture issues
        state.armPose = AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
    }

    @Override
    public ResourceLocation getTextureLocation(IllagerRenderState state) {
        return TEXTURE;
    }
}
