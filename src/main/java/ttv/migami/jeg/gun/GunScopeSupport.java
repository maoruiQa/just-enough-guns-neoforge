package ttv.migami.jeg.gun;

import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;

public final class GunScopeSupport {
    private static volatile boolean boltActionRifleScopeEnabled = false;

    private GunScopeSupport() {
    }

    public static boolean isBoltActionRifleScopeEnabled() {
        return boltActionRifleScopeEnabled;
    }

    public static boolean isBoltActionRifleScopeEnabled(ItemStack stack) {
        return GunAttachments.has(stack, AttachmentType.SCOPE);
    }

    public static void setBoltActionRifleScopeEnabled(boolean enabled) {
        boltActionRifleScopeEnabled = enabled;
    }
}
