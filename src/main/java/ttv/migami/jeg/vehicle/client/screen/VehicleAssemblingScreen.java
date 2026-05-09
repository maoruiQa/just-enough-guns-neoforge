package ttv.migami.jeg.vehicle.client.screen;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import ttv.migami.jeg.network.NetworkHandler;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipe;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;

public final class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {
    private static final int RECIPES_PER_PAGE = 6;
    private static final Component PREVIOUS_PAGE = Component.literal("<");
    private static final Component NEXT_PAGE = Component.literal(">");

    private List<VehicleAssemblyRecipe> recipes = List.of();
    private int page;

    public VehicleAssemblingScreen(VehicleAssemblingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 248;
        this.inventoryLabelY = 154;
    }

    @Override
    protected void init() {
        super.init();
        this.recipes = VehicleAssemblyRecipeManager.recipes();
        this.page = Math.min(this.page, this.maxPage());
        this.addRecipeButtons();
        this.addPageButtons();
    }

    private void addRecipeButtons() {
        int start = this.page * RECIPES_PER_PAGE;
        int end = Math.min(this.recipes.size(), start + RECIPES_PER_PAGE);
        for (int index = start; index < end; index++) {
            VehicleAssemblyRecipe recipe = this.recipes.get(index);
            int row = index - start;
            this.addRenderableWidget(Button.builder(vehicleName(recipe.resultVehicle()), button -> NetworkHandler.sendAssembleVehicle(recipe.id()))
                    .bounds(this.leftPos + 12, this.topPos + 24 + row * 24, 152, 20)
                    .build());
        }
    }

    private void addPageButtons() {
        if (this.maxPage() <= 0) {
            return;
        }
        this.addRenderableWidget(Button.builder(PREVIOUS_PAGE, button -> {
                    this.page = Math.max(0, this.page - 1);
                    this.refreshWidgets();
                })
                .bounds(this.leftPos + 116, this.topPos + 4, 22, 18)
                .build());
        this.addRenderableWidget(Button.builder(NEXT_PAGE, button -> {
                    this.page = Math.min(this.maxPage(), this.page + 1);
                    this.refreshWidgets();
                })
                .bounds(this.leftPos + 142, this.topPos + 4, 22, 18)
                .build());
    }

    private void refreshWidgets() {
        this.clearWidgets();
        this.init();
    }

    private int maxPage() {
        return Math.max(0, (this.recipes.size() - 1) / RECIPES_PER_PAGE);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xDD20252B);
        guiGraphics.fill(x + 6, y + 18, x + this.imageWidth - 6, y + 172, 0xAA111418);
        guiGraphics.fill(x + 6, y + 162, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xAA111418);
        int start = this.page * RECIPES_PER_PAGE;
        int end = Math.min(this.recipes.size(), start + RECIPES_PER_PAGE);
        for (int index = start; index < end; index++) {
            int row = index - start;
            guiGraphics.drawString(this.font, costText(this.recipes.get(index)), x + 12, y + 15 + row * 24, 0xFFE6E6E6);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static Component vehicleName(ResourceLocation vehicleId) {
        return Component.translatable("entity." + vehicleId.getNamespace() + "." + vehicleId.getPath());
    }

    private static Component costText(VehicleAssemblyRecipe recipe) {
        MutableComponent cost = Component.literal("Cost: ");
        for (int index = 0; index < recipe.ingredients().size(); index++) {
            VehicleAssemblyRecipe.Ingredient ingredient = recipe.ingredients().get(index);
            Item item = BuiltInRegistries.ITEM.get(ingredient.item());
            if (index > 0) {
                cost.append(Component.literal(", "));
            }
            cost.append(Component.literal(ingredient.count() + "x "));
            cost.append(item.getDescription());
        }
        return cost;
    }
}
