package ttv.migami.jeg.vehicle.client.render.item;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.vehicle.client.model.item.VehicleAssemblingTableItemModel;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;

public final class VehicleAssemblingTableBlockItemRenderer extends GeoItemRenderer<VehicleAssemblingTableBlockItem> {
    public VehicleAssemblingTableBlockItemRenderer() {
        super(new VehicleAssemblingTableItemModel());
    }

    @Override
    public RenderType getRenderType(VehicleAssemblingTableBlockItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
