package ttv.migami.jeg.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ttv.migami.jeg.item.GunItem;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void jeg$handlePlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }

        // Replace "swap hands" with reload when holding a gun (NeoForge parity).
        boolean reloaded = tryReload(InteractionHand.MAIN_HAND) | tryReload(InteractionHand.OFF_HAND);
        if (reloaded) {
            ci.cancel();
        }
    }

    private boolean tryReload(InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof GunItem gun)) {
            return false;
        }

        boolean reloaded = gun.tryReload(player.level(), player, stack, true);
        if (reloaded) {
            player.swing(hand, true);
        }
        return reloaded;
    }
}

