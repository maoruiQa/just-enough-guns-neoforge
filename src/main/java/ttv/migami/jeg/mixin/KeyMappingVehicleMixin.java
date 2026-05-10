package ttv.migami.jeg.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

@Mixin(KeyMapping.class)
public class KeyMappingVehicleMixin {
    @Shadow
    private InputConstants.Key key;

    @Shadow
    private int clickCount;

    @Inject(method = "consumeClick()Z", at = @At("HEAD"), cancellable = true)
    private void jeg$consumeVehicleHotbarClick(CallbackInfoReturnable<Boolean> callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VehicleEntity vehicle)) {
            return;
        }
        for (KeyMapping hotbarKey : Minecraft.getInstance().options.keyHotbarSlots) {
            if (hotbarKey.getKey() == this.key && vehicle.shouldBanPassengerHand(player)) {
                if (this.clickCount > 0) {
                    this.clickCount--;
                }
                callback.setReturnValue(false);
                return;
            }
        }
    }
}
