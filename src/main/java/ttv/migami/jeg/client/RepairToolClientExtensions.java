package ttv.migami.jeg.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import ttv.migami.jeg.client.render.item.RepairToolItemRenderer;

public final class RepairToolClientExtensions implements IClientItemExtensions {
    private BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new RepairToolItemRenderer();
        }
        return renderer;
    }
}
