package ttv.migami.jeg.vehicle.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.init.ModMenuTypes;

public final class VehicleMenu extends AbstractContainerMenu {
    public static final int MAX_VEHICLE_SLOT_COUNT = 54;
    private static final int DEFAULT_VEHICLE_SLOT_COUNT = 9;

    private final Container vehicleInventory;
    private final int vehicleSlotCount;
    private final int playerInventoryY;

    public VehicleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, DEFAULT_VEHICLE_SLOT_COUNT);
    }

    public VehicleMenu(int containerId, Inventory playerInventory, int vehicleSlotCount) {
        this(containerId, playerInventory, new SimpleContainer(vehicleSlotCount), vehicleSlotCount);
    }

    public VehicleMenu(int containerId, Inventory playerInventory, Container vehicleInventory) {
        this(containerId, playerInventory, vehicleInventory, Math.min(vehicleInventory.getContainerSize(), MAX_VEHICLE_SLOT_COUNT));
    }

    public VehicleMenu(int containerId, Inventory playerInventory, Container vehicleInventory, int vehicleSlotCount) {
        super(ModMenuTypes.VEHICLE_MENU.get(), containerId);
        this.vehicleSlotCount = Math.max(0, Math.min(vehicleSlotCount, MAX_VEHICLE_SLOT_COUNT));
        checkContainerSize(vehicleInventory, this.vehicleSlotCount);
        this.vehicleInventory = vehicleInventory;
        this.vehicleInventory.startOpen(playerInventory.player);
        int rows = this.vehicleRows();
        this.playerInventoryY = 20 + rows * 18 + 24;

        for (int slot = 0; slot < this.vehicleSlotCount; slot++) {
            this.addSlot(new Slot(vehicleInventory, slot, 8 + slot % 9 * 18, 20 + slot / 9 * 18));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, this.playerInventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, this.playerInventoryY + 58));
        }
    }

    public int vehicleRows() {
        return (this.vehicleSlotCount + 8) / 9;
    }

    public int playerInventoryY() {
        return this.playerInventoryY;
    }

    public int screenHeight() {
        return this.playerInventoryY + 82;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            int playerInventoryStart = this.vehicleSlotCount;
            int hotbarEnd = playerInventoryStart + 36;
            if (index < this.vehicleSlotCount) {
                if (!this.moveItemStackTo(stack, playerInventoryStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, this.vehicleSlotCount, false)) {
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
        return this.vehicleInventory.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.vehicleInventory.stopOpen(player);
    }
}
