package ttv.migami.jeg.vehicle.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;

public final class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {
    private static final ResourceLocation TEST_WHEEL_RECIPE = Reference.id("test_wheel_vehicle");
    private static final ResourceLocation LIGHT_COMBAT_RECIPE = Reference.id("light_combat_vehicle");
    private static final ResourceLocation TEST_HELICOPTER_RECIPE = Reference.id("test_helicopter");
    private static final ResourceLocation TEST_BOAT_RECIPE = Reference.id("test_boat");
    private static final ResourceLocation TEST_ARTILLERY_RECIPE = Reference.id("test_artillery");
    private static final ResourceLocation TEST_AIRCRAFT_RECIPE = Reference.id("test_aircraft");
    private static final Component ASSEMBLE_TEST = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_vehicle");
    private static final Component ASSEMBLE_LIGHT_COMBAT = Component.translatable("gui.jeg.vehicle_assembling.assemble_light_combat_vehicle");
    private static final Component ASSEMBLE_TEST_HELICOPTER = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_helicopter");
    private static final Component ASSEMBLE_TEST_BOAT = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_boat");
    private static final Component ASSEMBLE_TEST_ARTILLERY = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_artillery");
    private static final Component ASSEMBLE_TEST_AIRCRAFT = Component.translatable("gui.jeg.vehicle_assembling.assemble_test_aircraft");
    private static final Component TEST_COST = Component.translatable("gui.jeg.vehicle_assembling.test_vehicle_cost");
    private static final Component LIGHT_COMBAT_COST = Component.translatable("gui.jeg.vehicle_assembling.light_combat_vehicle_cost");
    private static final Component TEST_HELICOPTER_COST = Component.translatable("gui.jeg.vehicle_assembling.test_helicopter_cost");
    private static final Component TEST_BOAT_COST = Component.translatable("gui.jeg.vehicle_assembling.test_boat_cost");
    private static final Component TEST_ARTILLERY_COST = Component.translatable("gui.jeg.vehicle_assembling.test_artillery_cost");
    private static final Component TEST_AIRCRAFT_COST = Component.translatable("gui.jeg.vehicle_assembling.test_aircraft_cost");

    public VehicleAssemblingScreen(VehicleAssemblingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 248;
        this.inventoryLabelY = 154;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(ASSEMBLE_TEST, button -> NetworkHandler.sendAssembleVehicle(TEST_WHEEL_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 24, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(ASSEMBLE_LIGHT_COMBAT, button -> NetworkHandler.sendAssembleVehicle(LIGHT_COMBAT_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 48, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(ASSEMBLE_TEST_HELICOPTER, button -> NetworkHandler.sendAssembleVehicle(TEST_HELICOPTER_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 72, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(ASSEMBLE_TEST_BOAT, button -> NetworkHandler.sendAssembleVehicle(TEST_BOAT_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 96, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(ASSEMBLE_TEST_ARTILLERY, button -> NetworkHandler.sendAssembleVehicle(TEST_ARTILLERY_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 120, 152, 20)
                .build());
        this.addRenderableWidget(Button.builder(ASSEMBLE_TEST_AIRCRAFT, button -> NetworkHandler.sendAssembleVehicle(TEST_AIRCRAFT_RECIPE))
                .bounds(this.leftPos + 12, this.topPos + 144, 152, 20)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        guiGraphics.fill(x + 6, y + 18, x + this.imageWidth - 6, y + 172, 0xAA111418);
        guiGraphics.fill(x + 6, y + 162, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xAA111418);
        guiGraphics.drawString(this.font, TEST_COST, x + 12, y + 19, 0xFFE6E6E6);
        guiGraphics.drawString(this.font, LIGHT_COMBAT_COST, x + 12, y + 43, 0xFFE6E6E6);
        guiGraphics.drawString(this.font, TEST_HELICOPTER_COST, x + 12, y + 67, 0xFFE6E6E6);
        guiGraphics.drawString(this.font, TEST_BOAT_COST, x + 12, y + 91, 0xFFE6E6E6);
        guiGraphics.drawString(this.font, TEST_ARTILLERY_COST, x + 12, y + 115, 0xFFE6E6E6);
        guiGraphics.drawString(this.font, TEST_AIRCRAFT_COST, x + 12, y + 139, 0xFFE6E6E6);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
