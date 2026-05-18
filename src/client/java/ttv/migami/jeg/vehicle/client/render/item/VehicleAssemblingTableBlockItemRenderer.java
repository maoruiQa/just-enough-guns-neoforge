package ttv.migami.jeg.vehicle.client.render.item;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.vehicle.client.model.item.VehicleAssemblingTableItemModel;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;

public final class VehicleAssemblingTableBlockItemRenderer extends GeoItemRenderer<VehicleAssemblingTableBlockItem> {
    public VehicleAssemblingTableBlockItemRenderer() {
        super(new VehicleAssemblingTableItemModel());
    }

    @Override
    public RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }
}
