package ttv.migami.jeg.vehicle.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.Config;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.util.HudMessageHelper;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipe;
import ttv.migami.jeg.vehicle.recipe.VehicleAssemblyRecipeManager;

public final class VehicleAssemblingMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_END = 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final ContainerLevelAccess access;

    public VehicleAssemblingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public VehicleAssemblingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 166 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 224));
        }
    }

    public boolean assembleVehicle(Player player, Identifier recipeId) {
        if (!Config.vehiclesEnabled()) {
            HudMessageHelper.showActionBar(player, Component.translatable("message.jeg.vehicle.disabled"));
            return false;
        }
        VehicleAssemblyRecipe recipe = VehicleAssemblyRecipeManager.get(recipeId);
        if (recipe == null || !this.hasCost(recipe)) {
            player.sendSystemMessage(Component.translatable("message.jeg.vehicle_assembling.missing_materials"));
            return false;
        }
        this.removeCost(recipe);
        ItemStack result = VehicleContainerBlockEntity.createItemForVehicle(recipe.resultVehicle());
        this.giveOrDropResult(player, result);
        player.sendSystemMessage(Component.translatable("message.jeg.vehicle_assembling.completed"));
        return true;
    }

    private void giveOrDropResult(Player player, ItemStack result) {
        ItemStack remaining = result.copy();
        player.getInventory().add(remaining);
        if (remaining.isEmpty()) {
            return;
        }
        if (!this.dropAtTable(remaining)) {
            player.drop(remaining, false);
        }
    }

    private boolean dropAtTable(ItemStack stack) {
        final boolean[] dropped = {false};
        this.access.execute((level, pos) -> {
            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
            dropped[0] = true;
        });
        return dropped[0];
    }

    private boolean hasCost(VehicleAssemblyRecipe recipe) {
        for (VehicleAssemblyRecipe.Ingredient ingredient : recipe.ingredients()) {
            Item item = BuiltInRegistries.ITEM.getValue(ingredient.item());
            if (!this.hasCost(item, ingredient.count())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCost(Item item, int required) {
        int found = 0;
        for (int slot = 0; slot < this.playerInventory.getContainerSize(); slot++) {
            ItemStack stack = this.playerInventory.getItem(slot);
            if (stack.is(item)) {
                found += stack.getCount();
                if (found >= required) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeCost(VehicleAssemblyRecipe recipe) {
        for (VehicleAssemblyRecipe.Ingredient ingredient : recipe.ingredients()) {
            this.removeCost(BuiltInRegistries.ITEM.getValue(ingredient.item()), ingredient.count());
        }
    }

    private void removeCost(Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < this.playerInventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = this.playerInventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) {
                this.playerInventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= removed;
        }
        this.playerInventory.setChanged();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < PLAYER_INVENTORY_END) {
                if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ModBlocks.VEHICLE_ASSEMBLING_TABLE.get());
    }
}
