package ttv.migami.jeg.client.render.gun;

import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.entity.monster.phantom.gunner.PhantomGunner;
import software.bernie.geckolib.constant.dataticket.DataTicket;

public final class JegDataTickets {
    public static final DataTicket<ItemStack> ITEM_STACK = DataTicket.create("jeg:item_stack", ItemStack.class);
    public static final DataTicket<PhantomGunner> PHANTOM_GUNNER = DataTicket.create("jeg:phantom_gunner", PhantomGunner.class);

    private JegDataTickets() {
    }
}
