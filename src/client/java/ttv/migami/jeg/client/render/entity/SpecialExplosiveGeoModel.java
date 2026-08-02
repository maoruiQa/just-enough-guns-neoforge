package ttv.migami.jeg.client.render.entity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.entity.PlacedExplosiveEntity;
import ttv.migami.jeg.item.SpecialExplosiveItem;

public final class SpecialExplosiveGeoModel extends GeoModel<PlacedExplosiveEntity> {
    @Override
    public ResourceLocation getModelResource(PlacedExplosiveEntity animatable) {
        return switch (animatable.kind()) {
            case C4 -> Reference.id("geo/special/c4.geo.json");
            case CLAYMORE -> Reference.id("geo/special/claymore.geo.json");
            case TM_62 -> Reference.id("geo/special/tm_62.geo.json");
        };
    }

    @Override
    public ResourceLocation getTextureResource(PlacedExplosiveEntity animatable) {
        return switch (animatable.kind()) {
            case C4 -> Reference.id("textures/item/c4.png");
            case CLAYMORE -> Reference.id("textures/entity/claymore.png");
            case TM_62 -> Reference.id("textures/entity/tm_62.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(PlacedExplosiveEntity animatable) {
        // Empty animation file avoids null-path issues in some GeckoLib versions
        return Reference.id("animations/special/empty.animation.json");
    }
}
