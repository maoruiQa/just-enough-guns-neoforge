package ttv.migami.jeg.vehicle.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class DefaultVehicleResource {
    private static final String MODEL_ROOT = "geo/entity/vehicle/";
    private static final String TEXTURE_ROOT = "textures/entity/vehicle/";
    private static final String ANIMATION_ROOT = "animations/entity/vehicle/";
    private static final String FALLBACK = "generic_vehicle";
    private static final ResourceLocation FALLBACK_MODEL = Reference.id(MODEL_ROOT + FALLBACK + ".geo.json");
    private static final ResourceLocation FALLBACK_ANIMATION = Reference.id(ANIMATION_ROOT + FALLBACK + ".animation.json");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    private DefaultVehicleResource() {}

    public static ResourceLocation model(VehicleEntity vehicle) {
        ResourceLocation model = Reference.id(MODEL_ROOT + vehiclePath(vehicle) + ".geo.json");
        if (exists(model)) {
            return model;
        }
        ResourceLocation typeModel = Reference.id(MODEL_ROOT + typeFallback(vehicle) + ".geo.json");
        return exists(typeModel) ? typeModel : FALLBACK_MODEL;
    }

    public static ResourceLocation texture(VehicleEntity vehicle) {
        ResourceLocation texture = Reference.id(TEXTURE_ROOT + vehiclePath(vehicle) + ".png");
        return exists(texture) ? texture : FALLBACK_TEXTURE;
    }

    public static ResourceLocation animation(VehicleEntity vehicle) {
        ResourceLocation animation = Reference.id(ANIMATION_ROOT + vehiclePath(vehicle) + ".animation.json");
        return exists(animation) ? animation : FALLBACK_ANIMATION;
    }

    public static ResourceLocation glowTexture(VehicleEntity vehicle) {
        return Reference.id(TEXTURE_ROOT + vehiclePath(vehicle) + "_glow.png");
    }

    public static boolean hasGlowTexture(VehicleEntity vehicle) {
        return exists(glowTexture(vehicle));
    }

    private static String vehiclePath(VehicleEntity vehicle) {
        String path = vehicle.vehicleDataId().getPath();
        return path == null || path.isBlank() ? FALLBACK : path;
    }

    private static String typeFallback(VehicleEntity vehicle) {
        VehicleType type = vehicle.vehicleData().defaults().vehicleType();
        return switch (type) {
            case BOAT -> "boat_vehicle";
            case HELICOPTER -> "helicopter_vehicle";
            case AIRCRAFT -> "aircraft_vehicle";
            case ARTILLERY -> "artillery_vehicle";
            case LAND -> "land_vehicle";
        };
    }

    private static boolean exists(ResourceLocation id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }
}
