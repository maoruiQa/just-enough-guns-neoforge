package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    public VehicleScreen(VehicleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = menu.screenHeight();
        this.inventoryLabelY = menu.playerInventoryY() - 12;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        if (this.menu.vehicleRows() > 0) {
            guiGraphics.fill(x + 6, y + 16, x + this.imageWidth - 6, y + 24 + this.menu.vehicleRows() * 18, 0xAA111418);
        }
        guiGraphics.fill(x + 6, y + this.menu.playerInventoryY() - 4, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xAA111418);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
