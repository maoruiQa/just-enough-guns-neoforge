package ttv.migami.jeg.vehicle.client.model.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.item.VehicleAssemblingTableBlockItem;

public final class VehicleAssemblingTableItemModel extends GeoModel<VehicleAssemblingTableBlockItem> {
    @Override
    public ResourceLocation getAnimationResource(VehicleAssemblingTableBlockItem animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(VehicleAssemblingTableBlockItem animatable) {
        return Reference.id("geo/vehicle_assembling_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleAssemblingTableBlockItem animatable) {
        return Reference.id("textures/block/vehicle_assembling_table.png");
    }
}
