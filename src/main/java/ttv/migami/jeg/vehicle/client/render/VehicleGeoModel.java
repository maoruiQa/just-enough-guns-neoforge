package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoModel extends GeoModel<VehicleEntity> {
    private static final ResourceLocation MODEL = Reference.id("geo/entity/vehicle/generic_vehicle.geo.json");
    private static final ResourceLocation ANIMATION = Reference.id("animations/entity/vehicle/generic_vehicle.animation.json");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    @Override
    public ResourceLocation getModelResource(VehicleEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VehicleEntity animatable) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VehicleEntity animatable) {
        return ANIMATION;
    }
}
