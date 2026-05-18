package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.vehicle.menu.VehicleChargingStationMenu;

public final class VehicleChargingStationScreen extends AbstractContainerScreen<VehicleChargingStationMenu> {
    private static final Component STATUS_IDLE = Component.translatable("gui.jeg.vehicle_charging_station.idle");
    private static final Component STATUS_CHARGING = Component.translatable("gui.jeg.vehicle_charging_station.charging");
    private static final Component NO_TARGET = Component.translatable("gui.jeg.vehicle_charging_station.no_target");

    public VehicleChargingStationScreen(VehicleChargingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 78);
        this.inventoryLabelY = 1000;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        guiGraphics.fill(x + 8, y + 24, x + this.imageWidth - 8, y + 36, 0xAA111418);

        int maxEnergy = this.menu.maxVehicleEnergy();
        if (maxEnergy <= 0) {
            guiGraphics.text(this.font, NO_TARGET, x + 10, y + 44, 0xFFE6E6E6);
            super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        int energy = this.menu.vehicleEnergy();
        int fill = (int) ((this.imageWidth - 20) * Math.min(1.0F, energy / (float) maxEnergy));
        guiGraphics.fill(x + 10, y + 26, x + 10 + fill, y + 34, 0xFF4FC3F7);
        guiGraphics.text(this.font, Component.translatable("gui.jeg.vehicle_charging_station.energy", energy, maxEnergy), x + 10, y + 44, 0xFFE6E6E6);
        guiGraphics.text(this.font, this.menu.isCharging() ? STATUS_CHARGING : STATUS_IDLE, x + 10, y + 56, 0xFFE6E6E6);
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
    }
}
