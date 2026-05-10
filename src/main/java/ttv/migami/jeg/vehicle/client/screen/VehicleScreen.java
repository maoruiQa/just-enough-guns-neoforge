package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    private static final int TEXTURE_X_OFFSET = 8;
    private static final ResourceLocation MINI = Reference.id("textures/gui/vehicle/inventory/mini.png");
    private static final ResourceLocation SMALL = Reference.id("textures/gui/vehicle/inventory/small.png");
    private static final ResourceLocation MEDIUM = Reference.id("textures/gui/vehicle/inventory/medium.png");
    private static final ResourceLocation LARGE = Reference.id("textures/gui/vehicle/inventory/large.png");
    private static final ResourceLocation HUGE = Reference.id("textures/gui/vehicle/inventory/huge.png");
    private static final ResourceLocation PLAYER_INVENTORY = Reference.id("textures/gui/vehicle/inventory/player_inventory.png");

    public VehicleScreen(VehicleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = menu.vehicleColumns() > 13 ? 320 : 222;
        this.imageHeight = menu.screenHeight();
        this.titleLabelX = 15;
        this.titleLabelY = 5;
        this.inventoryLabelX = menu.playerInventoryXOffset() + this.titleLabelX;
        this.inventoryLabelY = menu.playerInventoryY() - 12;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        ResourceLocation texture = this.vehicleInventoryTexture();
        if (texture != null) {
            int textureSize = this.menu.vehicleColumns() > 13 ? 328 : 256;
            guiGraphics.blit(texture, x + TEXTURE_X_OFFSET, y, 0, 0, this.imageWidth, this.imageHeight, textureSize, textureSize);
        } else {
            guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        }
        guiGraphics.blit(PLAYER_INVENTORY, x + TEXTURE_X_OFFSET + this.menu.playerInventoryXOffset(), y + this.menu.playerInventoryY() - 8, 0, 0, 175, 90, 256, 256);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private ResourceLocation vehicleInventoryTexture() {
        if (this.menu.vehicleRows() <= 0) {
            return null;
        }
        if (this.menu.vehicleColumns() == 17) {
            return HUGE;
        }
        if (this.menu.vehicleColumns() == 13) {
            return LARGE;
        }
        return switch (this.menu.vehicleRows()) {
            case 1 -> MINI;
            case 3 -> SMALL;
            case 6 -> MEDIUM;
            default -> null;
        };
    }
}
