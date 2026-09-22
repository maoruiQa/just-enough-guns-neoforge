package ttv.migami.jeg.mixin;

import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.init.ModDataComponents;
import ttv.migami.jeg.item.GunItem;

@Mixin(FirstPersonHandsAndItems.class)
public class FirstPersonHandsAndItemsMixin {
    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float mainHandHeight;

    @Shadow
    private float oMainHandHeight;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Unique
    private ItemStack jeg$preTickMainHandItem = ItemStack.EMPTY;

    @Unique
    private ItemStack jeg$preTickOffHandItem = ItemStack.EMPTY;

    @Inject(method = "tick", at = @At("HEAD"))
    private void jeg$capturePreTickHandItems(LocalPlayer player, CallbackInfo ci) {
        this.jeg$preTickMainHandItem = this.mainHandItem.copy();
        this.jeg$preTickOffHandItem = this.offHandItem.copy();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jeg$stabilizeVolatileGunSwaps(LocalPlayer player, CallbackInfo ci) {
        ItemStack liveMain = player.getMainHandItem();
        if (jeg$isVolatileGunComponentDiff(this.mainHandItem, liveMain)
                || jeg$isVolatileGunComponentDiff(this.jeg$preTickMainHandItem, liveMain)) {
            this.mainHandItem = liveMain;
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
        }

        ItemStack liveOff = player.getOffhandItem();
        if (jeg$isVolatileGunComponentDiff(this.offHandItem, liveOff)
                || jeg$isVolatileGunComponentDiff(this.jeg$preTickOffHandItem, liveOff)) {
            this.offHandItem = liveOff;
            this.offHandHeight = 1.0F;
            this.oOffHandHeight = 1.0F;
        }
    }

    @Unique
    private static boolean jeg$isVolatileGunComponentDiff(ItemStack visibleStack, ItemStack liveStack) {
        if (visibleStack == null || liveStack == null || visibleStack.isEmpty() || liveStack.isEmpty()) {
            return false;
        }
        if (!ItemStack.isSameItem(visibleStack, liveStack)) {
            return false;
        }
        if (!(liveStack.getItem() instanceof GunItem)) {
            return false;
        }
        if (ItemStack.isSameItemSameComponents(visibleStack, liveStack)) {
            return false;
        }

        ItemStack visibleStable = visibleStack.copy();
        ItemStack liveStable = liveStack.copy();
        jeg$stripVolatileGunRenderComponents(visibleStable);
        jeg$stripVolatileGunRenderComponents(liveStable);
        return ItemStack.isSameItemSameComponents(visibleStable, liveStable);
    }

    @Unique
    private static void jeg$stripVolatileGunRenderComponents(ItemStack stack) {
        stack.remove(DataComponents.DAMAGE);
        stack.remove(ModDataComponents.GUN_AMMO.get());
        stack.remove(ModDataComponents.GUN_HEAT.get());
        stack.remove(ModDataComponents.GUN_TRIGGER_LOCK.get());
        stack.remove(ModDataComponents.GUN_RELOAD_STAGE.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_TOTAL.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_RELOAD_END_TICK.get());
        stack.remove(ModDataComponents.GUN_RELOAD_FROM_MAGAZINE_ITEM.get());
        stack.remove(ModDataComponents.GUN_RELOAD_TO_MAGAZINE_ITEM.get());
        stack.remove(ModDataComponents.GUN_DRAW_TICKS_REMAINING.get());
        stack.remove(ModDataComponents.GUN_SCOPE_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_BARREL_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_STOCK_ATTACHMENT_DAMAGE.get());
        stack.remove(ModDataComponents.GUN_UNDER_BARREL_ATTACHMENT_DAMAGE.get());
    }
}
