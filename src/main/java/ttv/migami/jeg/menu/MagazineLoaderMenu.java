package ttv.migami.jeg.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import ttv.migami.jeg.block.entity.MagazineLoaderBlockEntity;
import ttv.migami.jeg.init.ModBlocks;
import ttv.migami.jeg.init.ModMenuTypes;
import ttv.migami.jeg.item.MagazineItem;

public final class MagazineLoaderMenu extends AbstractContainerMenu {
    public static final int SLOT_AMMO = 0;
    public static final int SLOT_MAGAZINE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_COUNT = 1;

    private static final int MACHINE_SLOT_START = 0;
    private static final int MACHINE_SLOT_END = 3;
    private static final int PLAYER_INVENTORY_START = 3;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public MagazineLoaderMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOT_COUNT), new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
    }

    public MagazineLoaderMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModMenuTypes.MAGAZINE_LOADER_MENU.get(), containerId);
        checkContainerSize(container, SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.container = container;
        this.data = data;
        this.access = access;

        this.addSlot(new Slot(container, SLOT_AMMO, 56, 17) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return MagazineLoaderBlockEntity.isAmmo(stack);
            }
        });
        this.addSlot(new Slot(container, SLOT_MAGAZINE, 56, 53) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof MagazineItem;
            }
        });
        this.addSlot(new Slot(container, SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(data);
    }

    public int progressPixels() {
        int progress = this.data.get(DATA_PROGRESS);
        return progress <= 0 ? 0 : progress * 24 / MagazineLoaderBlockEntity.LOAD_PROGRESS_REQUIRED;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return copy;
        }

        ItemStack stack = slot.getItem();
        copy = stack.copy();
        if (index >= MACHINE_SLOT_START && index < MACHINE_SLOT_END) {
            if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof MagazineItem) {
            if (!this.moveItemStackTo(stack, SLOT_MAGAZINE, SLOT_MAGAZINE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (MagazineLoaderBlockEntity.isAmmo(stack)) {
            if (!this.moveItemStackTo(stack, SLOT_AMMO, SLOT_AMMO + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END && !this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, ModBlocks.MAGAZINE_LOADER.get());
    }
}
