package ttv.migami.jeg.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.item.GunItem;

@Mixin(Minecraft.class)
public final class MinecraftMixin {
    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void jeg$cancelVanillaGunAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null) {
            return;
        }

        ItemStack mainHand = this.player.getMainHandItem();
        if (mainHand.getItem() instanceof GunItem) {
            cir.setReturnValue(false);
        }
    }
}
