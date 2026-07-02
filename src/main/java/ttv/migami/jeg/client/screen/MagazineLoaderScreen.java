package ttv.migami.jeg.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.menu.MagazineLoaderMenu;

public final class MagazineLoaderScreen extends AbstractContainerScreen<MagazineLoaderMenu> {
    public MagazineLoaderScreen(MagazineLoaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        guiGraphics.fill(x + 55, y + 16, x + 73, y + 34, 0xFF12161A);
        guiGraphics.fill(x + 55, y + 52, x + 73, y + 70, 0xFF12161A);
        guiGraphics.fill(x + 115, y + 34, x + 133, y + 52, 0xFF12161A);
        guiGraphics.fill(x + 78, y + 38, x + 104, y + 46, 0xFF111418);
        int progress = this.menu.progressPixels();
        if (progress > 0) {
            guiGraphics.fill(x + 79, y + 39, x + 79 + progress, y + 45, 0xFFE0B15A);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
