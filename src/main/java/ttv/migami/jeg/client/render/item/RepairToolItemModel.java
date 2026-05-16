package ttv.migami.jeg.client.render.item;

import net.minecraft.resources.Identifier;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.RepairToolItem;

public final class RepairToolItemModel extends GeoModel<RepairToolItem> {
    @Override
    public Identifier getAnimationResource(RepairToolItem animatable) {
        return Reference.id("animations/repair_tool.animation.json");
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Reference.id("item/repair_tool");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Reference.id("textures/item/repair_tool.png");
    }
}
