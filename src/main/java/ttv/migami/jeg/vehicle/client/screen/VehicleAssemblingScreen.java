package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;

public final class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {
    private static final Component ASSEMBLE = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_vehicle");
    private static final Component COST = Component.translatable("gui.jeg.vehicle_assembling.test_vehicle_cost");

    public VehicleAssemblingScreen(VehicleAssemblingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 152;
        this.inventoryLabelY = 58;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(ASSEMBLE, button -> NetworkHandler.sendAssembleTestVehicle())
                .bounds(this.leftPos + 12, this.topPos + 28, 152, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        guiGraphics.fill(x + 6, y + 18, x + this.imageWidth - 6, y + 54, 0xAA111418);
        guiGraphics.fill(x + 6, y + 66, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xAA111418);
        guiGraphics.drawString(this.font, COST, x + 12, y + 19, 0xFFE6E6E6);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
