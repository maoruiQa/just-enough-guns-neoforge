package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.fabric.compat.neoforge.api.distmarker.Dist;
import ttv.migami.jeg.fabric.compat.neoforge.bus.api.SubscribeEvent;
import ttv.migami.jeg.fabric.compat.neoforge.fml.common.EventBusSubscriber;
import ttv.migami.jeg.fabric.compat.neoforge.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.ClientNetworkHandler;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class ClientGunInputHandler {
    private ClientGunInputHandler() {}

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.getAction() != GLFW.GLFW_RELEASE) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof GunItem gun)) {
                continue;
            }
            if (GunItem.isAutomatic(gun.getStats())) {
                continue;
            }
            if (!GunItem.isTriggerLocked(stack)) {
                continue;
            }
            GunItem.clearTriggerLock(stack);
            ClientNetworkHandler.sendTriggerRelease(hand);
        }
    }
}
