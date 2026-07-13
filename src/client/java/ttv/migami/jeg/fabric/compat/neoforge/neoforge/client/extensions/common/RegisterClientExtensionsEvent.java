package ttv.migami.jeg.fabric.compat.neoforge.neoforge.client.extensions.common;

import net.minecraft.world.item.Item;

public class RegisterClientExtensionsEvent {
    public void registerItem(IClientItemExtensions extensions, Item... items) {
        if (items == null) {
            return;
        }
        for (Item item : items) {
            IClientItemExtensions.register(item, extensions);
        }
    }
}
