package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.GunItem;
import ttv.migami.jeg.network.NetworkHandler;

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
            NetworkHandler.sendTriggerRelease(hand);
        }
    }
}
