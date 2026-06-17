package ttv.migami.jeg.vehicle.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.Reference;
import ttv.migami.jeg.network.ClientNetworkHandler;
import ttv.migami.jeg.vehicle.data.VehicleDataManager;
import ttv.migami.jeg.vehicle.data.subdata.VehicleType;
import ttv.migami.jeg.vehicle.menu.VehicleAssemblingMenu;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipe;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;

public final class VehicleAssemblingScreen extends AbstractContainerScreen<VehicleAssemblingMenu> {
    private static final int RECIPES_PER_PAGE = 6;
    private static final Component ALL_FILTER = Component.translatable("gui.jeg.vehicle_assembling.filter.all");
    private static final Component PREVIOUS_PAGE = Component.literal("<");
    private static final Component NEXT_PAGE = Component.literal(">");
    private static final int PREVIEW_X = 176;
    private static final int PREVIEW_Y = 24;
    private static final int PREVIEW_SIZE = 64;

    private List<VehicleAssemblyRecipe> allRecipes = List.of();
    private List<VehicleAssemblyRecipe> recipes = List.of();
    private int page;
    @Nullable
    private VehicleType filterType;
    private ResourceLocation previewVehicleId;
    private Entity previewEntity;

    public VehicleAssemblingScreen(VehicleAssemblingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 248;
        this.inventoryLabelY = 154;
    }

    @Override
    protected void init() {
        super.init();
        this.allRecipes = VehicleAssemblyRecipeManager.recipes();
        this.recipes = this.filteredRecipes();
        this.page = Math.min(this.page, this.maxPage());
        this.resetPreview();
        this.addFilterButtons();
        this.addRecipeButtons();
        this.addPageButtons();
    }

    private void addFilterButtons() {
        int x = this.leftPos + 10;
        int y = this.topPos + 4;
        this.addRenderableWidget(Button.builder(ALL_FILTER, button -> this.setFilter(null))
                .bounds(x, y, 28, 18)
                .build());
        x += 31;
        for (FilterButton filter : FilterButton.values()) {
            this.addRenderableWidget(Button.builder(filter.label, button -> this.setFilter(filter.type))
                    .bounds(x, y, 34, 18)
                    .build());
            x += 37;
        }
    }

    private void addRecipeButtons() {
        int start = this.page * RECIPES_PER_PAGE;
        int end = Math.min(this.recipes.size(), start + RECIPES_PER_PAGE);
        for (int index = start; index < end; index++) {
            VehicleAssemblyRecipe recipe = this.recipes.get(index);
            int row = index - start;
            Button button = Button.builder(vehicleName(recipe.resultVehicle()), clicked -> ClientNetworkHandler.sendAssembleVehicle(recipe.id()))
                    .bounds(this.leftPos + 12, this.topPos + 24 + row * 24, 152, 20)
                    .build();
            button.active = this.hasCost(recipe);
            this.addRenderableWidget(button);
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
                .bounds(this.leftPos + 196, this.topPos + 176, 22, 18)
                .build());
        this.addRenderableWidget(Button.builder(NEXT_PAGE, button -> {
                    this.page = Math.min(this.maxPage(), this.page + 1);
                    this.refreshWidgets();
                })
                .bounds(this.leftPos + 222, this.topPos + 176, 22, 18)
                .build());
    }

    private void refreshWidgets() {
        this.clearWidgets();
        this.init();
    }

    private void setFilter(@Nullable VehicleType type) {
        if (this.filterType == type) {
            return;
        }
        this.filterType = type;
        this.page = 0;
        this.refreshWidgets();
    }

    private List<VehicleAssemblyRecipe> filteredRecipes() {
        if (this.filterType == null) {
            return this.allRecipes;
        }
        return this.allRecipes.stream()
                .filter(recipe -> VehicleDataManager.get(recipe.resultVehicle()).defaults().vehicleType() == this.filterType)
                .toList();
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
        guiGraphics.fill(x + PREVIEW_X - 6, y + PREVIEW_Y - 6, x + this.imageWidth - 10, y + PREVIEW_Y + PREVIEW_SIZE + 12, 0xCC20252B);
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
        VehicleAssemblyRecipe previewRecipe = this.previewRecipe(mouseX, mouseY);
        if (previewRecipe != null) {
            this.renderVehicleIcon(guiGraphics, previewRecipe.resultVehicle());
            this.renderVehiclePreview(guiGraphics, previewRecipe, partialTick);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderVehicleIcon(GuiGraphics guiGraphics, ResourceLocation vehicleId) {
        ResourceLocation icon = vehicleIcon(vehicleId);
        if (icon == null) {
            return;
        }
        guiGraphics.blit(icon, this.leftPos + PREVIEW_X, this.topPos + PREVIEW_Y - 18, 64, 16, 0.0F, 0.0F, 256, 64, 256, 64);
    }

    private void renderVehiclePreview(GuiGraphics guiGraphics, VehicleAssemblyRecipe recipe, float partialTick) {
        Entity entity = this.previewEntity(recipe.resultVehicle());
        if (entity == null) {
            return;
        }
        entity.setYRot((this.minecraft == null ? 0.0F : this.minecraft.player == null ? 0.0F : this.minecraft.player.tickCount + partialTick) * 1.5F);
        entity.yRotO = entity.getYRot();

        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(this.leftPos + PREVIEW_X + PREVIEW_SIZE / 2.0F, this.topPos + PREVIEW_Y + PREVIEW_SIZE - 8.0F, 80.0F);
        poseStack.scale(22.0F, 22.0F, -22.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(35.0F));
        RenderSystem.runAsFancy(() -> {
            dispatcher.setRenderShadow(false);
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, entity.getYRot(), partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT);
            buffer.endBatch();
            dispatcher.setRenderShadow(true);
        });
        poseStack.popPose();
    }

    private Entity previewEntity(ResourceLocation vehicleId) {
        if (this.previewEntity != null && vehicleId.equals(this.previewVehicleId)) {
            return this.previewEntity;
        }
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        ResourceLocation entityTypeId = ResourceLocation.parse(VehicleDataManager.get(vehicleId).defaults().entityType());
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        Entity entity = type.create(this.minecraft.level);
        if (entity == null) {
            return null;
        }
        this.previewVehicleId = vehicleId;
        this.previewEntity = entity;
        return entity;
    }

    private VehicleAssemblyRecipe previewRecipe(int mouseX, int mouseY) {
        if (this.recipes.isEmpty()) {
            return null;
        }
        int start = this.page * RECIPES_PER_PAGE;
        int end = Math.min(this.recipes.size(), start + RECIPES_PER_PAGE);
        if (mouseX >= this.leftPos + 12 && mouseX < this.leftPos + 164) {
            for (int index = start; index < end; index++) {
                int row = index - start;
                int top = this.topPos + 24 + row * 24;
                if (mouseY >= top && mouseY < top + 20) {
                    return this.recipes.get(index);
                }
            }
        }
        return this.recipes.get(Math.min(start, this.recipes.size() - 1));
    }

    private void resetPreview() {
        this.previewVehicleId = null;
        this.previewEntity = null;
    }

    private static Component vehicleName(ResourceLocation vehicleId) {
        return Component.translatable("entity." + vehicleId.getNamespace() + "." + vehicleId.getPath());
    }

    @Nullable
    private static ResourceLocation vehicleIcon(ResourceLocation vehicleId) {
        return switch (vehicleId.getPath()) {
            case "a10", "ah6", "bmp2", "hpj11", "laser_tower", "lav150", "mi28", "speedboat", "tom6", "truck", "waveforce_tower" ->
                    Reference.id("textures/vehicle_icon/" + vehicleId.getPath() + "_icon.png");
            default -> null;
        };
    }

    private static Component costText(VehicleAssemblyRecipe recipe) {
        MutableComponent cost = Component.translatable("gui.jeg.vehicle_assembling.cost");
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

    private boolean hasCost(VehicleAssemblyRecipe recipe) {
        Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null) {
            return false;
        }
        for (VehicleAssemblyRecipe.Ingredient ingredient : recipe.ingredients()) {
            Item item = BuiltInRegistries.ITEM.get(ingredient.item());
            if (this.countItem(player, item) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private int countItem(Player player, Item item) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private enum FilterButton {
        LAND(VehicleType.LAND, Component.translatable("gui.jeg.vehicle_assembling.filter.land")),
        HELICOPTER(VehicleType.HELICOPTER, Component.translatable("gui.jeg.vehicle_assembling.filter.helicopter")),
        AIRCRAFT(VehicleType.AIRCRAFT, Component.translatable("gui.jeg.vehicle_assembling.filter.aircraft")),
        BOAT(VehicleType.BOAT, Component.translatable("gui.jeg.vehicle_assembling.filter.boat")),
        ARTILLERY(VehicleType.ARTILLERY, Component.translatable("gui.jeg.vehicle_assembling.filter.artillery"));

        private final VehicleType type;
        private final Component label;

        FilterButton(VehicleType type, Component label) {
            this.type = type;
            this.label = label;
        }
    }
}
