package ttv.migami.jeg.vehicle.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.vehicle.block.entity.VehicleContainerBlockEntity;

public final class VehicleAssemblingMenu extends AbstractContainerMenu {
    private static final int IRON_COST = 16;
    private static final int REDSTONE_COST = 4;

    private final Inventory playerInventory;

    public VehicleAssemblingMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), containerId);
        this.playerInventory = playerInventory;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 70 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 128));
        }
    }

    public boolean assembleTestVehicle(Player player) {
        if (!this.hasCost(Items.IRON_INGOT, IRON_COST) || !this.hasCost(Items.REDSTONE, REDSTONE_COST)) {
            player.displayClientMessage(Component.translatable("message.jeg.vehicle_assembling.missing_materials"), true);
            return false;
        }
        this.removeCost(Items.IRON_INGOT, IRON_COST);
        this.removeCost(Items.REDSTONE, REDSTONE_COST);
        ItemStack result = VehicleContainerBlockEntity.createDefaultItem();
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
        player.displayClientMessage(Component.translatable("message.jeg.vehicle_assembling.completed"), true);
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
