package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.DroneEntity;

public final class DroneGeoModel extends GeoModel<DroneEntity> {
    public static final DataTicket<DroneEntity> ANIMATABLE =
            DataTicket.create("jeg_drone_animatable", DroneEntity.class);

    private static final Identifier MODEL = Reference.id("special/drone");
    private static final Identifier TEXTURE = Reference.id("textures/entity/drone.png");

    @Override
    public void addAdditionalStateData(DroneEntity animatable, Object obj, GeoRenderState renderState) {
        renderState.addGeckolibData(ANIMATABLE, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(DroneEntity animatable) {
        return null;
    }
}
