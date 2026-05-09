package ttv.migami.jeg.vehicle.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.vehicle.entity.base.VehicleEntity;

public final class VehicleMenu extends AbstractContainerMenu {
    public static final int MAX_VEHICLE_SLOT_COUNT = 102;
    private static final int DEFAULT_VEHICLE_SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 18;
    private static final int VEHICLE_SLOT_X = 16;
    private static final int VEHICLE_SLOT_Y = 18;
    private static final int PLAYER_SLOT_Y = 104;
    private static final int HOTBAR_SLOT_Y = 162;

    private final Container vehicleInventory;
    private final int vehicleSlotCount;
    private final int vehicleRows;
    private final int vehicleColumns;
    private final int playerInventoryY;
    @Nullable
    private final VehicleEntity vehicle;

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
        this(containerId, playerInventory, vehicleInventory, vehicleSlotCount, null);
    }

    public VehicleMenu(int containerId, Inventory playerInventory, Container vehicleInventory, int vehicleSlotCount, @Nullable VehicleEntity vehicle) {
        super(ModMenuTypes.VEHICLE_MENU.get(), containerId);
        this.vehicleSlotCount = Math.max(0, Math.min(vehicleSlotCount, MAX_VEHICLE_SLOT_COUNT));
        checkContainerSize(vehicleInventory, this.vehicleSlotCount);
        this.vehicleInventory = vehicleInventory;
        this.vehicle = vehicle;
        this.vehicleColumns = columnsFor(this.vehicleSlotCount);
        this.vehicleRows = this.vehicleColumns == 0 ? 0 : (this.vehicleSlotCount + this.vehicleColumns - 1) / this.vehicleColumns;
        this.vehicleInventory.startOpen(playerInventory.player);
        int rowOffset = (this.vehicleRows - 4) * SLOT_SIZE;
        int columnOffset = this.playerInventoryXOffset();
        this.playerInventoryY = PLAYER_SLOT_Y + rowOffset;

        for (int slot = 0; slot < this.vehicleSlotCount; slot++) {
            this.addSlot(new Slot(vehicleInventory, slot, VEHICLE_SLOT_X + slot % this.vehicleColumns * SLOT_SIZE, VEHICLE_SLOT_Y + slot / this.vehicleColumns * SLOT_SIZE));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, VEHICLE_SLOT_X + columnOffset + col * SLOT_SIZE, this.playerInventoryY + row * SLOT_SIZE));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, VEHICLE_SLOT_X + columnOffset + col * SLOT_SIZE, HOTBAR_SLOT_Y + rowOffset));
        }
    }

    public int vehicleRows() {
        return this.vehicleRows;
    }

    public int vehicleColumns() {
        return this.vehicleColumns;
    }

    public int playerInventoryXOffset() {
        return Math.max(0, (this.vehicleColumns - 9) / 2 * SLOT_SIZE);
    }

    public int playerInventoryY() {
        return this.playerInventoryY;
    }

    public int screenHeight() {
        return 222;
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
        if (this.vehicle != null) {
            return this.vehicle.isAlive() && player.distanceToSqr(this.vehicle) <= 64.0D;
        }
        return this.vehicleInventory.stillValid(player);
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.vehicleInventory.stopOpen(player);
    }

    private static int columnsFor(int slotCount) {
        if (slotCount <= 0) {
            return 0;
        }
        if (slotCount > 78) {
            return 17;
        }
        if (slotCount > 54) {
            return 13;
        }
        return 9;
    }
}
