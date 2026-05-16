package ttv.migami.jeg.vehicle.client.render.block;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import ttv.migami.jeg.vehicle.block.VehicleAssemblingTableBlock;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;
import ttv.migami.jeg.vehicle.block.property.BlockPart;
import ttv.migami.jeg.vehicle.client.model.block.VehicleAssemblingTableBlockModel;
import ttv.migami.jeg.vehicle.client.render.layer.VehicleAssemblingTableBlockLayer;

public final class VehicleAssemblingTableBlockEntityRenderer extends GeoBlockRenderer<VehicleAssemblingTableBlockEntity> {
    public VehicleAssemblingTableBlockEntityRenderer() {
        super(new VehicleAssemblingTableBlockModel());
        this.addRenderLayer(new VehicleAssemblingTableBlockLayer(this));
    }

    @Override
    public RenderType getRenderType(VehicleAssemblingTableBlockEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public boolean shouldRender(VehicleAssemblingTableBlockEntity blockEntity, @NotNull Vec3 cameraPos) {
        BlockPart part = blockEntity.getBlockState().getValue(VehicleAssemblingTableBlock.BLOCK_PART);
        if (part == BlockPart.FLB) {
            return true;
        }
        return false;
    }

    @Override
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
