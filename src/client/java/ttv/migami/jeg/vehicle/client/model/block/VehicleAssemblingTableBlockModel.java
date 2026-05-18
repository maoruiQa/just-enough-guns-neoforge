package ttv.migami.jeg.vehicle.client.model.block;

import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.block.entity.VehicleAssemblingTableBlockEntity;

public final class VehicleAssemblingTableBlockModel extends GeoModel<VehicleAssemblingTableBlockEntity> {
    @Override
    public Identifier getAnimationResource(VehicleAssemblingTableBlockEntity animatable) {
        return null;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Reference.id("block/vehicle_assembling_table");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Reference.id("textures/block/vehicle_assembling_table.png");
    }
}
