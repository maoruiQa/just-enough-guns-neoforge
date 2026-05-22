package ttv.migami.jeg.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.item.ItemStack;
import ttv.migami.jeg.item.HappyGhastArmorHelper;
import ttv.migami.jeg.vehicle.client.overlay.VehicleHudOverlay;

public final class HappyGhastArmorHud {
    private HappyGhastArmorHud() {}

    public static void render(GuiGraphicsExtractor guiGraphics) {
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
