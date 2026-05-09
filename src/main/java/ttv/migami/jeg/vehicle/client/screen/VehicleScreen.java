package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    private static final ResourceLocation PLAYER_INVENTORY = Reference.id("textures/gui/vehicle/inventory/player_inventory.png");

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
        guiGraphics.blit(PLAYER_INVENTORY, x, y + this.menu.playerInventoryY() - 4, 0, 0, 175, 90, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
