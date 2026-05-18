package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.vehicle.client.resource.DefaultVehicleResource;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoModel extends GeoModel<VehicleEntity> {
    static final DataTicket<VehicleEntity> ANIMATABLE =
            DataTicket.create("jeg_generic_vehicle_animatable", VehicleEntity.class);

    @Override
    public void addAdditionalStateData(VehicleEntity animatable, Object obj, GeoRenderState renderState) {
        renderState.addGeckolibData(ANIMATABLE, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        VehicleEntity animatable = renderState.getOrDefaultGeckolibData(ANIMATABLE, (VehicleEntity) null);
        return DefaultVehicleResource.model(animatable);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        VehicleEntity animatable = renderState.getOrDefaultGeckolibData(ANIMATABLE, (VehicleEntity) null);
        return DefaultVehicleResource.texture(animatable);
    }

    @Override
    public Identifier getAnimationResource(VehicleEntity animatable) {
        return DefaultVehicleResource.animation(animatable);
    }
}
