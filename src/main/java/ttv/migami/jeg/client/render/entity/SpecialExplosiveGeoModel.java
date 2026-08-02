package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.Identifier;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.item.SpecialExplosiveItem;

public final class SpecialExplosiveGeoModel extends GeoModel<PlacedExplosiveEntity> {
    public static final DataTicket<PlacedExplosiveEntity> ANIMATABLE =
            DataTicket.create("jeg_placed_explosive_animatable", PlacedExplosiveEntity.class);

    @Override
    public void addAdditionalStateData(PlacedExplosiveEntity animatable, Object obj, GeoRenderState renderState) {
        renderState.addGeckolibData(ANIMATABLE, animatable);
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        PlacedExplosiveEntity entity = renderState.hasGeckolibData(ANIMATABLE) ? renderState.getGeckolibData(ANIMATABLE) : null;
        return modelFor(entity);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        PlacedExplosiveEntity entity = renderState.hasGeckolibData(ANIMATABLE) ? renderState.getGeckolibData(ANIMATABLE) : null;
        return textureFor(entity);
    }

    @Override
    public Identifier getAnimationResource(PlacedExplosiveEntity animatable) {
        return null;
    }

    private static Identifier modelFor(PlacedExplosiveEntity entity) {
        SpecialExplosiveItem.Kind kind = entity == null ? SpecialExplosiveItem.Kind.C4 : entity.kind();
        return switch (kind) {
            case CLAYMORE -> Reference.id("special/claymore");
            case TM_62 -> Reference.id("special/tm_62");
            case C4 -> Reference.id("special/c4");
        };
    }

    private static Identifier textureFor(PlacedExplosiveEntity entity) {
        SpecialExplosiveItem.Kind kind = entity == null ? SpecialExplosiveItem.Kind.C4 : entity.kind();
        return switch (kind) {
            case CLAYMORE -> Reference.id("textures/entity/claymore.png");
            case TM_62 -> Reference.id("textures/entity/tm_62.png");
            case C4 -> Reference.id("textures/entity/c4.png");
        };
    }
}
