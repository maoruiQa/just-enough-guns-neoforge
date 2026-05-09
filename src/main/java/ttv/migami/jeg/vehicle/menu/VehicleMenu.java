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
    public static final int VEHICLE_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_START = VEHICLE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container vehicleInventory;

    public VehicleMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(VEHICLE_SLOT_COUNT));
    }

    public VehicleMenu(int containerId, Inventory playerInventory, Container vehicleInventory) {
        super(ModMenuTypes.VEHICLE_MENU.get(), containerId);
        checkContainerSize(vehicleInventory, VEHICLE_SLOT_COUNT);
        this.vehicleInventory = vehicleInventory;
        this.vehicleInventory.startOpen(playerInventory.player);

        for (int slot = 0; slot < VEHICLE_SLOT_COUNT; slot++) {
            this.addSlot(new Slot(vehicleInventory, slot, 8 + slot * 18, 20));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 62 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 120));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < VEHICLE_SLOT_COUNT) {
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, VEHICLE_SLOT_COUNT, false)) {
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
