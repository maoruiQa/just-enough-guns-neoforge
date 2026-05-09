package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class CombatScopeGeoModel extends GeoModel<AnimatedGunItem> {
    @Override
    public ResourceLocation getModelResource(AnimatedGunItem animatable) {
        return Reference.id("geo/item/attachment/combat_scope.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem animatable) {
        return Reference.id("textures/animated/attachment/combat_scope.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem animatable) {
        return Reference.id("animations/item/combat_scope.animation.json");
    }
}
