package ttv.migami.jeg.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.advancement.GameplayActions;
import ttv.migami.jeg.item.GunItem;

@Mixin(AnvilMenu.class)
public abstract class GunRepairAdvancementMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void jeg$repair(Player player, ItemStack result, CallbackInfo ci) {
        ItemStack original = ((AnvilMenu) (Object) this).getSlot(0).getItem();
        if (original.getItem() instanceof GunItem && result.is(original.getItem()) && result.getDamageValue() < original.getDamageValue()) {
            GameplayActions.action(player, "gun_repair");
        }
    }
}
