package ttv.migami.jeg.client.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBinds;
import ttv.migami.jeg.item.FlashlightItem;
import ttv.migami.jeg.network.PacketHandler;
import ttv.migami.jeg.network.message.C2SMessageFlashlight;

// @Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class FlashlightHandler
{
    private static FlashlightHandler instance;

    public static FlashlightHandler get()
    {
        if(instance == null)
        {
            instance = new FlashlightHandler();
        }
        return instance;
    }

    private FlashlightHandler()
    {
    }

    @SubscribeEvent
    public static void flashlight(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        Minecraft mc = Minecraft.getInstance();
        ItemStack heldItem = player.getMainHandItem();
        if(heldItem.getItem() instanceof FlashlightItem)
        {
            if (!Config.COMMON.gameplay.allowFlashlights.get()) {
                Component message = Component.translatable("chat.jeg.disabled_flashlights")
                        .withStyle(ChatFormatting.GRAY);
                player.displayClientMessage(message, true);
                return;
            }

            boolean charging = KeyBinds.getShootMapping().isDown();
            if(charging)
            {
                PacketHandler.getPlayChannel().sendToServer(new C2SMessageFlashlight());
                mc.options.keyAttack.setDown(false);
                KeyBinds.getShootMapping().setDown(false);
            }
        }
    }
}
