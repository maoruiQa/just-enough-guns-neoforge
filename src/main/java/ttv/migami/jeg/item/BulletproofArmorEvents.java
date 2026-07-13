package ttv.migami.jeg.item;

import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.fabric.compat.neoforge.bus.api.SubscribeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.fml.common.EventBusSubscriber;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.event.AnvilUpdateEvent;
import ttv.migami.jeg.Reference;

@EventBusSubscriber(modid = Reference.MOD_ID)
public final class BulletproofArmorEvents {
    private BulletproofArmorEvents() {}

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (BulletproofArmorItem.isBulletproof(left) || BulletproofArmorItem.isBulletproof(right)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCanceled(true);
        }
    }
}
