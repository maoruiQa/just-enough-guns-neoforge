package ttv.migami.jeg.client.render.gun;

import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.item.AnimatedGunItem;

public final class AnimatedGunRenderer extends GeoItemRenderer<AnimatedGunItem> {
    public AnimatedGunRenderer() {
        super(new AnimatedGunGeoModel());
    }
}

