package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.client.KeyBindings;
import ttv.migami.jeg.vehicle.client.VehicleInputHandler;
import ttv.migami.jeg.vehicle.menu.VehicleMenu;

public final class VehicleScreen extends AbstractContainerScreen<VehicleMenu> {
    private static final int TEXTURE_X_OFFSET = 8;
    private static final Identifier MINI = Reference.id("textures/gui/vehicle/inventory/mini.png");
    private static final Identifier SMALL = Reference.id("textures/gui/vehicle/inventory/small.png");
    private static final Identifier MEDIUM = Reference.id("textures/gui/vehicle/inventory/medium.png");
    private static final Identifier LARGE = Reference.id("textures/gui/vehicle/inventory/large.png");
    private static final Identifier HUGE = Reference.id("textures/gui/vehicle/inventory/huge.png");
    private static final Identifier PLAYER_INVENTORY = Reference.id("textures/gui/vehicle/inventory/player_inventory.png");

    public VehicleScreen(VehicleMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.vehicleColumns() > 13 ? 320 : 222, menu.screenHeight());
        this.titleLabelX = 15;
        this.titleLabelY = 5;
        this.inventoryLabelX = menu.playerInventoryXOffset() + this.titleLabelX;
        this.inventoryLabelY = menu.playerInventoryY() - 12;
    }

    @Override
    protected void init() {
        super.init();
        VehicleInputHandler.clearPendingVehicleInventoryClicks();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        Identifier texture = this.vehicleInventoryTexture();
        if (texture != null) {
            int textureSize = this.menu.vehicleColumns() > 13 ? 328 : 256;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + TEXTURE_X_OFFSET, y, 0, 0, this.imageWidth, this.imageHeight, textureSize, textureSize);
        } else {
            guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        }
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, PLAYER_INVENTORY, x + TEXTURE_X_OFFSET + this.menu.playerInventoryXOffset(), y + this.menu.playerInventoryY() - 8, 0, 0, 175, 90, 256, 256);
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyInventory.matches(event)) {
            if (VehicleInputHandler.shouldIgnoreVehicleInventoryKey()) {
                return true;
            }
            VehicleInputHandler.syncMouseToCurrentCursor();
            this.minecraft.player.closeContainer();
            this.minecraft.setScreen(null);
            VehicleInputHandler.clearPendingVehicleInventoryClicks();
            return true;
        }
        if (KeyBindings.VEHICLE_PLAYER_INVENTORY.matches(event)) {
            VehicleInputHandler.syncMouseToCurrentCursor();
            this.minecraft.player.closeContainer();
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
            VehicleInputHandler.clearPendingPlayerInventoryClicks();
            return true;
        }
        return super.keyPressed(event);
    }

    private Identifier vehicleInventoryTexture() {
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
