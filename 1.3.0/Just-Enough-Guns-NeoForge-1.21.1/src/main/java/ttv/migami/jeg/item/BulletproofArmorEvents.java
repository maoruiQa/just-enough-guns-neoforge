package ttv.migami.jeg.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
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
