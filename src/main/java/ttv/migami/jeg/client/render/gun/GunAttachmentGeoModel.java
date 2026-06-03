package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class GunAttachmentGeoModel extends GeoModel<AnimatedGunItem> {
    private static final ResourceLocation FALLBACK_MODEL = Reference.id("geo/item/attachment/combat_scope.geo.json");
    private static final ResourceLocation FALLBACK_TEXTURE = Reference.id("textures/animated/attachment/combat_scope.png");
    private static final ResourceLocation FALLBACK_ANIMATION = Reference.id("animations/item/combat_scope.animation.json");

    @Override
    public ResourceLocation getModelResource(AnimatedGunItem animatable) {
        return FALLBACK_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AnimatedGunItem animatable) {
        return FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AnimatedGunItem animatable) {
        return FALLBACK_ANIMATION;
    }
}
