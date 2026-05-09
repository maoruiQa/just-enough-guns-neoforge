package ttv.migami.jeg.vehicle.client.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleGeoModel extends GeoModel<VehicleEntity> {
    private static final String MODEL_ROOT = "geo/entity/vehicle/";
    private static final String TEXTURE_ROOT = "textures/entity/vehicle/";
    private static final String ANIMATION_ROOT = "animations/entity/vehicle/";
    private static final String FALLBACK = "generic_vehicle";
    private static final ResourceLocation FALLBACK_MODEL = Reference.id(MODEL_ROOT + FALLBACK + ".geo.json");
    private static final ResourceLocation FALLBACK_ANIMATION = Reference.id(ANIMATION_ROOT + FALLBACK + ".animation.json");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    @Override
    public ResourceLocation getModelResource(VehicleEntity animatable) {
        ResourceLocation model = Reference.id(MODEL_ROOT + path(animatable) + ".geo.json");
        return exists(model) ? model : FALLBACK_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VehicleEntity animatable) {
        ResourceLocation texture = Reference.id(TEXTURE_ROOT + path(animatable) + ".png");
        return exists(texture) ? texture : FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(VehicleEntity animatable) {
        ResourceLocation animation = Reference.id(ANIMATION_ROOT + path(animatable) + ".animation.json");
        return exists(animation) ? animation : FALLBACK_ANIMATION;
    }

    private static String path(VehicleEntity vehicle) {
        String path = vehicle.vehicleDataId().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

    private static boolean exists(ResourceLocation id) {
        return net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
