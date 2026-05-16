package ttv.migami.jeg.vehicle.client.model.item;

import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;

public final class VehicleAssemblingTableItemModel extends GeoModel<VehicleAssemblingTableBlockItem> {
    @Override
    public Identifier getAnimationResource(VehicleAssemblingTableBlockItem animatable) {
        return null;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Reference.id("item/vehicle_assembling_table");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Reference.id("textures/block/vehicle_assembling_table.png");
    }
}
