package ttv.migami.jeg.vehicle.client.model.block;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;

public final class VehicleAssemblingTableBlockModel extends GeoModel<VehicleAssemblingTableBlockEntity> {
    @Override
    public ResourceLocation getAnimationResource(VehicleAssemblingTableBlockEntity animatable) {
        return null;
    }

    @Override
    public ResourceLocation getModelResource(VehicleAssemblingTableBlockEntity animatable) {
        return Reference.id("geo/vehicle_assembling_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VehicleAssemblingTableBlockEntity animatable) {
        return Reference.id("textures/block/vehicle_assembling_table.png");
    }
}
