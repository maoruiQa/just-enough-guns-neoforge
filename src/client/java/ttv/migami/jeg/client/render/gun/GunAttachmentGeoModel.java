package ttv.migami.jeg.client.render.gun;

import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class GunAttachmentGeoModel extends GeoModel<AnimatedGunItem> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Reference.id("item/attachment/reflex_sight");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Reference.id("textures/animated/attachment/reflex_sight.png");
    }

    @Override
    public Identifier getAnimationResource(AnimatedGunItem animatable) {
        return Reference.id("item/attachment/reflex_sight");
    }
}
