package ttv.migami.jeg.vehicle.client.render.block;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.vehicle.block.VehicleAssemblingTableBlock;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.block.property.BlockPart;
import ttv.migami.jeg.vehicle.client.model.block.VehicleAssemblingTableBlockModel;
import ttv.migami.jeg.vehicle.client.render.layer.VehicleAssemblingTableBlockLayer;

public final class VehicleAssemblingTableBlockEntityRenderer extends GeoBlockRenderer<VehicleAssemblingTableBlockEntity, VehicleBlockRenderState> {
    public VehicleAssemblingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new VehicleAssemblingTableBlockModel());
        this.withRenderLayer(new VehicleAssemblingTableBlockLayer(this));
    }

    @Override
    public VehicleBlockRenderState createRenderState() {
        return new VehicleBlockRenderState();
    }

    @Override
    public void extractRenderState(VehicleAssemblingTableBlockEntity blockEntity, VehicleBlockRenderState renderState, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        super.extractRenderState(blockEntity, renderState, partialTick, cameraPos, crumblingOverlay);
        renderState.night = blockEntity.getLevel() != null && blockEntity.getLevel().getDefaultClockTime() % 24000L >= 13000L;
    }

    @Override
    public RenderType getRenderType(VehicleBlockRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRender(VehicleAssemblingTableBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        BlockPart part = blockEntity.getBlockState().getValue(VehicleAssemblingTableBlock.BLOCK_PART);
        if (part == BlockPart.FLB) {
            return true;
        }
        return false;
    }

    public @NotNull AABB getRenderBoundingBox(@NotNull VehicleAssemblingTableBlockEntity blockEntity) {
        double expansion = 2.0D;
        var worldPosition = blockEntity.getBlockPos();
        return new AABB(
                worldPosition.getX() - 1,
                worldPosition.getY(),
                worldPosition.getZ() - 1,
                worldPosition.getX() + 2,
                worldPosition.getY() + expansion,
                worldPosition.getZ() + 2
        );
    }
}
