package ttv.migami.jeg.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import ttv.migami.jeg.vehicle.client.render.item.VehicleAssemblingTableBlockItemRenderer;

public final class VehicleAssemblingTableClientExtensions implements IClientItemExtensions {
    private BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new VehicleAssemblingTableBlockItemRenderer();
        }
        return renderer;
    }
}
