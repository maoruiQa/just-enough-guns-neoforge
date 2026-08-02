package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.DroneEntity;

public final class DroneGeoModel extends GeoModel<DroneEntity> {
    private static final ResourceLocation MODEL = Reference.id("geo/special/drone.geo.json");
    private static final ResourceLocation TEXTURE = Reference.id("textures/entity/drone.png");

    @Override
    public ResourceLocation getModelResource(DroneEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DroneEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DroneEntity animatable) {
        return null;
    }
}
