package ttv.migami.jeg.gun;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.attachment.AttachmentType;
import ttv.migami.jeg.item.attachment.GunAttachments;

public final class GunScopeSupport {
    private static final net.minecraft.resources.ResourceLocation TELESCOPIC_SIGHT = Reference.id("telescopic_sight");
    private static volatile boolean boltActionRifleScopeEnabled = false;

    private GunScopeSupport() {
    }

    public static boolean isBoltActionRifleScopeEnabled() {
        return boltActionRifleScopeEnabled;
    }

    public static boolean isBoltActionRifleScopeEnabled(ItemStack stack) {
        return hasTelescopicSight(stack);
    }

    public static boolean hasTelescopicSight(ItemStack stack) {
        return GunAttachments.id(stack, AttachmentType.SCOPE)
                .filter(TELESCOPIC_SIGHT::equals)
                .isPresent();
    }

    public static boolean hasSpyglassScope(ItemStack stack) {
        return GunAttachments.stack(stack, AttachmentType.SCOPE)
                .map(ItemStack::getItem)
                .filter(SpyglassItem.class::isInstance)
                .isPresent();
    }

    public static void setBoltActionRifleScopeEnabled(boolean enabled) {
        boltActionRifleScopeEnabled = enabled;
    }
}
