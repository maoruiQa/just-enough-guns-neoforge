package ttv.migami.jeg.client.render.item;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import ttv.migami.jeg.item.RepairToolItem;

public final class RepairToolItemRenderer extends GeoItemRenderer<RepairToolItem> {
    public RepairToolItemRenderer() {
        super(new RepairToolItemModel());
    }

    @Override
    public RenderType getRenderType(RepairToolItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
