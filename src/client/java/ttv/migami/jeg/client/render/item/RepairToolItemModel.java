package ttv.migami.jeg.client.render.item;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.RepairToolItem;

public final class RepairToolItemModel extends GeoModel<RepairToolItem> {
    @Override
    public ResourceLocation getAnimationResource(RepairToolItem animatable) {
        return Reference.id("animations/repair_tool.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RepairToolItem animatable) {
        return Reference.id("geo/repair_tool.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RepairToolItem animatable) {
        return Reference.id("textures/item/repair_tool.png");
    }
}
