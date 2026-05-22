package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.item.HappyGhastArmorHelper;
import ttv.migami.jeg.vehicle.client.overlay.VehicleHudOverlay;

@EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public final class HappyGhastArmorHud {
    private HappyGhastArmorHud() {}

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName() == null || !"hotbar".equals(event.getName().getPath())) {
            return;
        }
        render(event.getGuiGraphics());
    }

    private static void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof HappyGhast ghast)) {
            return;
        }
        ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
        float max = HappyGhastArmorHelper.getMaxPlating(harness);
        if (max <= 0.0F) {
            return;
        }
        float ratio = Mth.clamp(HappyGhastArmorHelper.getPlating(harness) / max, 0.0F, 1.0F);
        VehicleHudOverlay.renderArmorValueBar(guiGraphics, guiGraphics.guiWidth() / 2 - 36, guiGraphics.guiHeight() - 59, ratio);
    }
}
