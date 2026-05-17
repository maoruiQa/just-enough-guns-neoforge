package ttv.migami.jeg.vehicle.client.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class DefaultVehicleResource {
    private static final String MODEL_ROOT = "entity/vehicle/";
    private static final String MODEL_LOD_ROOT = MODEL_ROOT + "lod/";
    private static final String TEXTURE_ROOT = "textures/entity/vehicle/";
    private static final String TEXTURE_LOD_ROOT = TEXTURE_ROOT + "lod/";
    private static final String ANIMATION_ROOT = "entity/vehicle/";
    private static final String FALLBACK = "generic_vehicle";
    private static final double LOD_1_DISTANCE = 32.0D;
    private static final double LOD_2_DISTANCE = 96.0D;
    private static final Identifier FALLBACK_MODEL = Reference.id(MODEL_ROOT + FALLBACK);
    private static final Identifier FALLBACK_ANIMATION = Reference.id(ANIMATION_ROOT + FALLBACK);
    private static final Identifier FALLBACK_TEXTURE = Identifier.withDefaultNamespace("textures/block/iron_block.png");

    private DefaultVehicleResource() {}

    public static Identifier model(VehicleEntity vehicle) {
        String vehiclePath = vehiclePath(vehicle);
        Identifier lodModel = lodModel(vehicle);
        if (lodModel != null) {
            return lodModel;
        }
        Identifier model = Reference.id(MODEL_ROOT + vehiclePath);
        if (modelExists(model)) {
            return model;
        }
        Identifier typeModel = Reference.id(MODEL_ROOT + typeFallback(vehicle));
        return modelExists(typeModel) ? typeModel : FALLBACK_MODEL;
    }

    public static Identifier texture(VehicleEntity vehicle) {
        String vehiclePath = vehiclePath(vehicle);
        Identifier lodTexture = lodResource(vehicle, TEXTURE_LOD_ROOT, ".png");
        if (lodTexture != null) {
            return lodTexture;
        }
        Identifier texture = Reference.id(TEXTURE_ROOT + vehiclePath + ".png");
        if (exists(texture)) {
            return texture;
        }
        Identifier typeTexture = Reference.id(TEXTURE_ROOT + typeFallback(vehicle) + ".png");
        return exists(typeTexture) ? typeTexture : FALLBACK_TEXTURE;
    }

    public static Identifier animation(VehicleEntity vehicle) {
        Identifier animation = Reference.id(ANIMATION_ROOT + vehiclePath(vehicle));
        if (animationExists(animation)) {
            return animation;
        }
        Identifier typeAnimation = Reference.id(ANIMATION_ROOT + typeFallback(vehicle));
        return animationExists(typeAnimation) ? typeAnimation : FALLBACK_ANIMATION;
    }

    public static Identifier glowTexture(VehicleEntity vehicle) {
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

    private static Identifier lodResource(VehicleEntity vehicle, String root, String suffix) {
        int lodLevel = lodLevel(vehicle);
        if (lodLevel <= 0) {
            return null;
        }
        String vehiclePath = vehiclePath(vehicle);
        for (int level = lodLevel; level >= 1; level--) {
            Identifier resource = Reference.id(root + vehiclePath + "_lod" + level + suffix);
            if (exists(resource)) {
                return resource;
            }
        }
        return null;
    }

    private static Identifier lodModel(VehicleEntity vehicle) {
        int lodLevel = lodLevel(vehicle);
        if (lodLevel <= 0) {
            return null;
        }
        String vehiclePath = vehiclePath(vehicle);
        for (int level = lodLevel; level >= 1; level--) {
            Identifier resource = Reference.id(MODEL_LOD_ROOT + vehiclePath + "_lod" + level);
            if (modelExists(resource)) {
                return resource;
            }
        }
        return null;
    }

    private static int lodLevel(VehicleEntity vehicle) {
        if (Minecraft.getInstance().player == null) {
            return 0;
        }
        double distance = Minecraft.getInstance().player.distanceTo(vehicle);
        if (distance <= LOD_1_DISTANCE) {
            return 0;
        }
        return distance <= LOD_2_DISTANCE ? 1 : 2;
    }

    private static boolean exists(Identifier id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }

    private static boolean modelExists(Identifier id) {
        return exists(Reference.id("geckolib/models/" + id.getPath() + ".geo.json"));
    }

    private static boolean animationExists(Identifier id) {
        return exists(Reference.id("geckolib/animations/" + id.getPath() + ".animation.json"));
    }
}
